package app.morphe.extension.chmate;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** On-demand, rate-limited search of Hissi's daily public poster indexes. */
public final class HissiWacchoiSearchActivity extends Activity {
    public static final String EXTRA_HALF = "app.morphe.extension.chmate.WACCHOI_HALF";
    public static final String HALF_UPPER = "upper";
    // The public daily index is served on HTTP; its HTTPS endpoint can time out.
    // Cleartext is enabled only for this host in hissi_network_security_config.xml.
    private static final String ORIGIN = "http://hissi.org";
    private static final String ZZZSEARCH_URL = "https://zzzsearch.com/2ch/";
    private static final String EDGE_SUBJECT =
            "https://bbs.eddibb.cc/liveedge/subject-metadent.txt";
    private static final String EDGE_DAT = "https://bbs.eddibb.cc/liveedge/dat/";
    private static final long MIN_INTERVAL_MS = 10_000L;
    private static final int RESPONSE_LIMIT = 2 * 1024 * 1024;
    private static final int MAX_LIST_PAGES = 1000;
    private static final int MAX_SEARCH_DAYS = 36525;
    private static final int MAX_PROFILES = 25;
    private static final int MAX_VISIBLE_BOARDS = 120;
    private static final Pattern BOARD_LINK = Pattern.compile(
            "(?is)<a\\b[^>]*href\\s*=\\s*['\\\"]?(?:https?://(?:www\\.)?hissi\\.org)?/read\\.php/([a-z0-9_-]+)/[^>]*>(.*?)</a>");
    private static final Pattern ROW = Pattern.compile("(?is)<tr\\b[^>]*>(.*?)</tr>");
    private static final Pattern PROFILE_LINK = Pattern.compile(
            "(?is)<a\\b[^>]*?href\\s*=\\s*['\\\"]?([^\\s'\\\">]+)['\\\"]?[^>]*>(.*?)</a>");
    private static final Pattern SLIP = WacchoiSearch.SLIP;
    private static final Pattern THREAD_LINK = Pattern.compile(
            "(?i)href=['\\\"]?(https?://[a-z0-9.-]+\\.5ch\\.(?:io|net)/test/read\\.cgi/[^\\s'\\\">]+)");
    private static final Pattern EDGE_SUBJECT_ROW = Pattern.compile(
            "(?m)^\\s*([0-9]{10,})\\.dat<>.*$");

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile Future<?> job;
    private volatile boolean cancelled;
    private volatile long lastRequestAt;
    private volatile HttpURLConnection activeConnection;
    private volatile SearchCursor resumeCursor;
    private final Set<String> displayedResults = new LinkedHashSet<>();
    private final Set<String> selectedBoards = new LinkedHashSet<>();
    private final List<BoardEntry> availableBoards = new ArrayList<>();
    private EditText boardFilter, fromInput, toInput, fourInput, pageLimitInput;
    private LinearLayout boardChoices, boardSection;
    private Button boardToggle;
    private TextView boardStatus;
    private RadioGroup halfGroup;
    private Button startButton, edgeStartButton, stopButton, resumeButton;
    private TextView statusView;
    private LinearLayout resultsLayout;
    private android.app.Dialog zzzDialog;
    private android.webkit.WebView zzzWebView;
    private final android.os.Handler backgroundUi=new android.os.Handler(android.os.Looper.getMainLooper());
    private WacchoiBackgroundJob observedBackground;
    private int backgroundRecordCount;
    private WacchoiHistory.Session localHistory;
    private boolean showingHistory;
    private final Runnable refreshBackground=new Runnable(){public void run(){
        WacchoiBackgroundJob current=WacchoiSearchService.current;
        if(current!=null && !showingHistory){
            if(observedBackground!=current){observedBackground=current;backgroundRecordCount=0;displayedResults.clear();resultsLayout.removeAllViews();}
            java.util.List<WacchoiBackgroundJob.Record> records=current.records();
            while(backgroundRecordCount<records.size()){
                WacchoiBackgroundJob.Record r=records.get(backgroundRecordCount++);
                if(r.kind==0)publishResult(r.a,r.b);
                else if(r.kind==1)publishThread(new HissiThreadResults.Result(r.a,r.b,r.c));
                else publishEdgeResult(r.a,r.b,r.c,r.d);
            }
            statusView.setText("バックグラウンド検索: "+current.status);
            startButton.setEnabled(!current.running);edgeStartButton.setEnabled(!current.running);
            stopButton.setEnabled(current.running);resumeButton.setEnabled(!current.running&&current.canResume());
        }
        backgroundUi.postDelayed(this,1000);
    }};
    @Override protected void onResume(){super.onResume();backgroundUi.post(refreshBackground);}
    @Override protected void onPause(){backgroundUi.removeCallbacks(refreshBackground);super.onPause();}
    private boolean backgroundEnabled(){return getSharedPreferences("io.github.areteruhiro.chmate.haiagaru.ui-config",0).getBoolean("wacchoiBackgroundSearch",false);}
    private boolean startBackground(Intent args){
        if(WacchoiSearchService.current!=null && WacchoiSearchService.current.running){
            Toast.makeText(this,"実行中の検索を停止してから開始してください",Toast.LENGTH_SHORT).show();return true;
        }
        if(!backgroundEnabled())return false;
        localHistory=null;
        showingHistory=false;
        WacchoiSearchService.launch(this,args);
        if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},4817);
        backgroundUi.removeCallbacks(refreshBackground);backgroundUi.postDelayed(refreshBackground,300);
        return true;
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        String half = getIntent().getStringExtra(EXTRA_HALF);
        if (HALF_UPPER.equals(half)) halfGroup.check(halfGroup.getChildAt(0).getId());
        String query = getIntent().getStringExtra("haiagaru.wacchoi.query");
        if (query != null && query.matches("-[A-Za-z0-9+/]{4}")) fourInput.setText(query.substring(1));
        if (query != null && query.matches("[A-Za-z0-9+/]{4}-")) {
            fourInput.setText(query.substring(0,4));halfGroup.check(halfGroup.getChildAt(0).getId());
        }
    }

    private void buildUi() {
        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("未取得の投稿をワッチョイ4桁で検索");
        title.setTextSize(19);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title, row());

        TextView note = new TextView(this);
        note.setText("端末で未取得の投稿も対象です。Hissiは板を複数選び、カレンダーで日付を指定して日別一覧を照合します。「全対応板を選択」でHissiの掲載板すべてを対象にできます。エッヂは現行スレのDATを照合します。上4桁／下4桁は英字の大小を区別し、+ と / にも対応します。通信は最低10秒間隔で、上限や停止後は続きから検索できます。Hissiは公開HTTPページを利用します。ZZZSearchは5ch・2ch.sc・過去ログ等の全文検索をアプリ内で開きます。結果に表記ゆれが混ざるためリンク先で確認してください。各検索サービスに未収録の投稿は検索できません。");
        note.setTextSize(13);
        root.addView(note, row());
        Button historyButton=new Button(this);historyButton.setText("上・下4桁の検索履歴・保存結果");
        historyButton.setOnClickListener(v->WacchoiHistory.show(this,this::openHistory));root.addView(historyButton,row());

        boardToggle = new Button(this);
        boardToggle.setAllCaps(false);
        boardToggle.setOnClickListener(v -> {
            boardSection.setVisibility(boardSection.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            updateBoardToggle();
        });
        root.addView(boardToggle, row());
        boardSection = new LinearLayout(this);
        boardSection.setOrientation(LinearLayout.VERTICAL);
        boardSection.setVisibility(View.GONE);
        root.addView(boardSection, row());
        updateBoardToggle();
        boardSection.addView(label("検索する板（複数選択できます）"));
        boardFilter = field("板名・カテゴリを絞り込む", "");
        boardSection.addView(boardFilter, row());
        LinearLayout boardActions = new LinearLayout(this);
        Button selectAllBoards = new Button(this);
        selectAllBoards.setText("表示中を全選択"); selectAllBoards.setAllCaps(false);
        Button selectEveryBoard = new Button(this);
        selectEveryBoard.setText("全対応板を選択"); selectEveryBoard.setAllCaps(false);
        Button clearBoards = new Button(this);
        clearBoards.setText("すべて解除"); clearBoards.setAllCaps(false);
        boardActions.addView(selectAllBoards, new LinearLayout.LayoutParams(0, dp(48), 1));
        boardActions.addView(selectEveryBoard, new LinearLayout.LayoutParams(0, dp(48), 1));
        boardActions.addView(clearBoards, new LinearLayout.LayoutParams(0, dp(48), 1));
        boardSection.addView(boardActions, row());
        selectEveryBoard.setOnClickListener(v -> {
            if (availableBoards.isEmpty()) {
                boardStatus.setText("板一覧の取得が終わってから選択してください");
                return;
            }
            WacchoiSearch.selectAllBoards(availableBoardCodes(), selectedBoards);
            renderBoardChoices();
        });
        boardStatus = label("板一覧を取得しています…");
        boardSection.addView(boardStatus, row());
        boardChoices = new LinearLayout(this);
        boardChoices.setOrientation(LinearLayout.VERTICAL);
        boardSection.addView(boardChoices, row());
        boardFilter.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderBoardChoices(); }
            @Override public void afterTextChanged(android.text.Editable s) { }
        });
        selectAllBoards.setOnClickListener(v -> {
            String filter = boardFilter.getText().toString().trim().toLowerCase(Locale.ROOT);
            int selected = 0;
            for (BoardEntry board : availableBoards) {
                if (filter.isEmpty() || board.label.toLowerCase(Locale.ROOT).contains(filter)
                        || board.code.toLowerCase(Locale.ROOT).contains(filter)) {
                    if (selected++ >= MAX_VISIBLE_BOARDS) break;
                    selectedBoards.add(board.code);
                }
            }
            renderBoardChoices();
        });
        clearBoards.setOnClickListener(v -> {
            selectedBoards.clear();
            renderBoardChoices();
        });

        fromInput = field("開始日を選択", today());
        toInput = field("終了日を選択", today());
        fromInput.setFocusable(false); fromInput.setClickable(true);
        toInput.setFocusable(false); toInput.setClickable(true);
        fromInput.setOnClickListener(v -> chooseDate(fromInput));
        toInput.setOnClickListener(v -> chooseDate(toInput));
        root.addView(label("対象日（カレンダーから選択、最大100年）"));
        LinearLayout dates = new LinearLayout(this);
        dates.setOrientation(LinearLayout.HORIZONTAL);
        dates.addView(fromInput, new LinearLayout.LayoutParams(0, dp(52), 1));
        dates.addView(toInput, new LinearLayout.LayoutParams(0, dp(52), 1));
        root.addView(dates, row());

        fourInput = field("例: Sdff、Q8ld、o+Mz", "");
        root.addView(label("照合する4文字（英字の大文字・小文字を区別）"));
        root.addView(fourInput, row());
        halfGroup = new RadioGroup(this);
        halfGroup.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton first = new RadioButton(this);
        first.setText("上4桁（ハイフン前：例 Sdff）"); first.setId(View.generateViewId());
        RadioButton last = new RadioButton(this);
        last.setText("下4桁（ハイフン後：例 Q8ld）"); last.setId(View.generateViewId());
        halfGroup.addView(first); halfGroup.addView(last); last.setChecked(true);
        root.addView(halfGroup, row());

        root.addView(label("ZZZSearchの追加条件（4桁を空にしてキーワードだけでも検索できます）"));
        EditText titleKeyword = field("スレ名のキーワード（任意）", "");
        EditText bodyKeyword = field("書き込み内容のキーワード（任意）", "");
        root.addView(titleKeyword, row());
        root.addView(bodyKeyword, row());
        root.addView(label("スレ名はタイトル条件、書き込み内容は全文条件として検索します。本文だけの厳密な照合ではありません。Hissi・エッヂDAT検索は4桁条件を使います。"));

        Button broadSearch = new Button(this);
        broadSearch.setText("ZZZSearchで検索（4桁・スレ名・書き込み内容）");
        broadSearch.setAllCaps(false);
        broadSearch.setOnClickListener(v -> {
            String token = fourInput.getText().toString().trim();
            String titleWords = titleKeyword.getText().toString().trim();
            String bodyWords = bodyKeyword.getText().toString().trim();
            if (!token.isEmpty() && !WacchoiSearch.validToken(token)) {
                fourInput.setError("英数字・+・/ の4文字を指定してください");
                return;
            }
            boolean upper = halfGroup.getCheckedRadioButtonId()
                    == halfGroup.getChildAt(0).getId();
            StringBuilder query = new StringBuilder();
            if (!token.isEmpty()) query.append('"').append(upper ? token + "-" : "-" + token).append('"');
            if (!titleWords.isEmpty()) query.append(" intitle:\"")
                    .append(titleWords.replace('"', ' ')).append('"');
            if (!bodyWords.isEmpty()) query.append(' ').append(bodyWords);
            if (query.toString().trim().isEmpty()) {
                Toast.makeText(this, "4桁・スレ名・書き込み内容のいずれかを入力してください", Toast.LENGTH_SHORT).show();
                return;
            }
            // Search the public cross-board index (s=5: all supported sources). The
            // service renders its own result list and can search 5ch, 2ch.sc, and
            // archived-log indexes without this app crawling those servers.
            openZzzSearch(ZZZSEARCH_URL + "?s=5&query=" + Uri.encode(query.toString().trim()));
        });
        root.addView(broadSearch, row());

        edgeStartButton = new Button(this);
        edgeStartButton.setText("エッヂ現行スレのDATを検索");
        edgeStartButton.setAllCaps(false);
        edgeStartButton.setOnClickListener(v -> startEdgeSearch());
        root.addView(edgeStartButton, row());

        pageLimitInput = field("一覧ページ上限（通信数の目安）", "20");
        root.addView(label("一覧ページ上限（最大1000）"));
        root.addView(pageLimitInput, row());

        LinearLayout buttons = new LinearLayout(this);
        startButton = new Button(this); startButton.setText("検索開始");
        stopButton = new Button(this); stopButton.setText("停止"); stopButton.setEnabled(false);
        buttons.addView(startButton, new LinearLayout.LayoutParams(0, dp(52), 1));
        buttons.addView(stopButton, new LinearLayout.LayoutParams(0, dp(52), 1));
        root.addView(buttons, row());

        resumeButton = new Button(this);
        resumeButton.setText("続きから検索");
        resumeButton.setEnabled(false);
        resumeButton.setOnClickListener(v -> resumeSearch());
        root.addView(resumeButton, row());

        statusView = new TextView(this); statusView.setTextSize(13);
        root.addView(statusView, row());
        resultsLayout = new LinearLayout(this); resultsLayout.setOrientation(LinearLayout.VERTICAL);
        root.addView(resultsLayout, row());
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);
        setContentView(scroll);

        startButton.setOnClickListener(v -> startSearch());
        stopButton.setOnClickListener(v -> stopSearch());
        loadBoards();
    }

    private void loadBoards() {
        boardStatus.setText("Hissiから板一覧を取得しています…");
        executor.submit(() -> {
            try {
                String html = fetch(ORIGIN + "/menu.php");
                Matcher matcher = BOARD_LINK.matcher(html);
                List<BoardEntry> loaded = new ArrayList<>();
                Set<String> seen = new LinkedHashSet<>();
                while (matcher.find()) {
                    String code = matcher.group(1);
                    if (!seen.add(code)) continue;
                    String label = Html.fromHtml(matcher.group(2), Html.FROM_HTML_MODE_LEGACY)
                            .toString().trim();
                    if (!label.isEmpty()) loaded.add(new BoardEntry(code, label));
                }
                if (loaded.isEmpty()) throw new IllegalStateException("板一覧を読み取れませんでした");
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    availableBoards.clear(); availableBoards.addAll(loaded);
                    if (selectedBoards.isEmpty() && seen.contains("android")) selectedBoards.add("android");
                    renderBoardChoices();
                    boardStatus.setText(availableBoards.size() + "板を取得しました（選択中 "
                            + selectedBoards.size() + "板）");
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    boardStatus.setText("板一覧を取得できません: " + error.getMessage()
                            + "。通信後に検索画面を開き直してください。");
                });
            }
        });
    }

    private void renderBoardChoices() {
        if (boardChoices == null) return;
        boardChoices.removeAllViews();
        String filter = boardFilter.getText().toString().trim().toLowerCase(Locale.ROOT);
        int shown = 0, matches = 0;
        for (BoardEntry board : availableBoards) {
            if (!filter.isEmpty() && !board.label.toLowerCase(Locale.ROOT).contains(filter)
                    && !board.code.toLowerCase(Locale.ROOT).contains(filter)) continue;
            matches++;
            if (shown >= MAX_VISIBLE_BOARDS) continue;
            CheckBox choice = new CheckBox(this);
            choice.setText(board.label + "  (" + board.code + ")");
            choice.setTag(board.code);
            choice.setChecked(selectedBoards.contains(board.code));
            choice.setOnCheckedChangeListener((button, checked) -> {
                if (checked) selectedBoards.add(board.code); else selectedBoards.remove(board.code);
                boardStatus.setText(availableBoards.size() + "板を取得しました（選択中 "
                        + selectedBoards.size() + "板）");
                updateBoardToggle();
            });
            boardChoices.addView(choice, row());
            shown++;
        }
        if (boardStatus != null && !availableBoards.isEmpty()) {
            boardStatus.setText(availableBoards.size() + "板中 " + matches + "板が一致、"
                    + shown + "板を表示（選択中 " + selectedBoards.size() + "板）"
                    + (matches > MAX_VISIBLE_BOARDS ? "。板名をさらに絞ると続きが表示されます" : ""));
        }
        updateBoardToggle();
    }

    private List<String> availableBoardCodes() {
        List<String> codes = new ArrayList<>(availableBoards.size());
        for (BoardEntry board : availableBoards) codes.add(board.code);
        return codes;
    }

    private void updateBoardToggle() {
        if (boardToggle == null || boardSection == null) return;
        boardToggle.setText("掲示板（" + selectedBoards.size() + "件選択）  "
                + (boardSection.getVisibility() == View.VISIBLE ? "▲" : "▼"));
    }

    private void chooseDate(EditText target) {
        Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.US);
            format.setLenient(false);
            Date current = format.parse(target.getText().toString());
            if (current != null) calendar.setTime(current);
        } catch (Exception ignored) { }
        new DatePickerDialog(this, (picker, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day);
            target.setText(new SimpleDateFormat("yyyyMMdd", Locale.US).format(selected.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void startSearch() {
        final String board;
        if (WacchoiSearch.allBoardsSelected(availableBoardCodes(), selectedBoards)) {
            // Preserve the all-board scope in the search cursor while avoiding
            // a long comma-separated board list in its scope identifier.
            board = "*";
        } else {
            StringBuilder boardBuilder = new StringBuilder();
            for (String selected : selectedBoards) {
                if (boardBuilder.length() > 0) boardBuilder.append(',');
                boardBuilder.append(selected);
            }
            board = boardBuilder.toString();
        }
        final String from = fromInput.getText().toString().trim();
        final String to = toInput.getText().toString().trim();
        final String four = fourInput.getText().toString().trim();
        final boolean firstHalf = halfGroup.getCheckedRadioButtonId() == halfGroup.getChildAt(0).getId();
        int parsedPageLimit;
        try { parsedPageLimit = Integer.parseInt(pageLimitInput.getText().toString().trim()); }
        catch (RuntimeException invalid) { parsedPageLimit = -1; }
        final int pageLimit = parsedPageLimit;
        if (board.isEmpty()) { boardStatus.setError("検索する板を1つ以上選択してください"); return; }
        if (!validDate(from) || !validDate(to) || to.compareTo(from) < 0
                || daysBetween(from, to) >= MAX_SEARCH_DAYS) {
            fromInput.setError("日付はYYYYMMDD、期間は最大100年です"); return;
        }
        if (!WacchoiSearch.validToken(four)) { fourInput.setError("英数字・+・/ の4文字を指定してください"); return; }
        if (pageLimit < 1 || pageLimit > MAX_LIST_PAGES) { pageLimitInput.setError("1〜1000を指定してください"); return; }

        if(startBackground(new Intent().putExtra("boards",board).putExtra("from",from).putExtra("to",to)
                .putExtra("four",four).putExtra("upper",firstHalf).putExtra("limit",pageLimit)))return;
        WacchoiSearchService.current=null;observedBackground=null;
        showingHistory=false;
        localHistory=WacchoiHistory.begin(this,new Intent().putExtra("boards",board).putExtra("from",from).putExtra("to",to)
                .putExtra("four",four).putExtra("upper",firstHalf).putExtra("limit",pageLimit));

        cancelled = false;
        resumeCursor = null;
        displayedResults.clear();
        resumeButton.setEnabled(false);
        startButton.setEnabled(false); edgeStartButton.setEnabled(false);
        stopButton.setEnabled(true); resultsLayout.removeAllViews();
        statusView.setText("開始しています。ページ取得は最低10秒間隔です。");
        job = executor.submit(() -> runSearch(board, from, to, four, firstHalf, pageLimit, null));
    }

    private void runSearch(String boardChoice, String from, String to, String four,
            boolean firstHalf, int pageLimit, SearchCursor resume) {
        int requests = 0, scanned = 0, profiles = 0, found = 0;
        boolean exhausted = true;
        Set<String> boardsDone = new LinkedHashSet<>();
        try {
            List<String> boards;
            if (resume != null) boards = resume.boards;
            else if (boardChoice.equals("*")) {
                String menu = fetch(ORIGIN + "/menu.php"); requests++;
                Matcher boardMatcher = BOARD_LINK.matcher(menu);
                boards = new ArrayList<>();
                while (boardMatcher.find()) boards.add(boardMatcher.group(1));
                boards = new ArrayList<>(new LinkedHashSet<>(boards));
                if (boards.isEmpty()) throw new IllegalStateException("Hissiの対応板一覧を読み取れませんでした");
            } else boards = new ArrayList<>(java.util.Arrays.asList(boardChoice.split(",")));

            Calendar day = Calendar.getInstance();
            day.setTime(new SimpleDateFormat("yyyyMMdd", Locale.US).parse(resume == null ? from : resume.date));
            Calendar end = Calendar.getInstance();
            end.setTime(new SimpleDateFormat("yyyyMMdd", Locale.US).parse(to));
            outer: for (; !day.after(end); day.add(Calendar.DATE, 1)) {
                String date = new SimpleDateFormat("yyyyMMdd", Locale.US).format(day.getTime());
                for (String board : boards) {
                    if (resume != null && date.equals(resume.date)
                            && boards.indexOf(board) < boards.indexOf(resume.board)) continue;
                    int page = resume != null && date.equals(resume.date) && board.equals(resume.board)
                            ? resume.page : 0;
                    resumeCursor = new SearchCursor(boards, boardChoice, date, to, board, page,
                            four, firstHalf, pageLimit, null, 0);
                    if (cancelled || scanned >= pageLimit) { exhausted = false; break outer; }
                    String base = ORIGIN + "/read.php/" + board + "/" + date + "/";
                    while (!cancelled && scanned < pageLimit) {
                        resumeCursor = new SearchCursor(boards, boardChoice, date, to, board, page,
                                four, firstHalf, pageLimit, null, 0);
                        String url = base + (page == 0 ? "" : "?p=" + page);
                        String body = fetch(url); requests++; scanned++; boardsDone.add(board);
                        List<String> next = scanRows(body, base, four, firstHalf);
                        for (String result : next) {
                            if (cancelled) throw new InterruptedException("停止しました");
                            found++;
                            publishResult(result, "一致 " + date + " / " + board);
                            if (profiles < MAX_PROFILES) {
                                String profileUrl = result.substring(0, result.indexOf('\n'));
                                String profile = fetch(profileUrl); requests++; profiles++;
                                for (HissiThreadResults.Result thread : HissiThreadResults.parse(profile)) {
                                    publishThread(thread);
                                }
                            }
                        }
                        publishStatus("日付 " + date + " / 板 " + board + " / 一覧 " + scanned
                                + "ページ / 通信 " + requests + "回 / 一致 " + found);
                        int nextNumber = WacchoiSearch.nextPage(body, page);
                        if (nextNumber < 0) break;
                        page = nextNumber;
                        resumeCursor = new SearchCursor(boards, boardChoice, date, to, board, page,
                                four, firstHalf, pageLimit, null, 0);
                        if (scanned >= pageLimit || cancelled) { exhausted = false; break outer; }
                    }
                }
            }
            if (exhausted && !cancelled) resumeCursor = null;
            String finalState = cancelled ? "停止しました" : resumeCursor != null
                    ? "ページ上限に達しました。「続きから検索」で再開できます"
                    : "検索範囲の確認が終わりました";
            publishStatus(finalState + "。対象板 " + boardsDone.size() + " / 日別一覧 " + scanned
                    + "ページ / 通信 " + requests + "回 / 一致 " + found
                    + "。これはHissiに収録された範囲です。");
        } catch (Exception error) {
            publishStatus((cancelled ? "停止しました" : "検索を中断しました") + ": " + error.getMessage()
                    + "。通信 " + requests + "回、一覧 " + scanned + "ページ。");
        } finally {
            runOnUiThread(() -> {
                startButton.setEnabled(true); edgeStartButton.setEnabled(true);
                stopButton.setEnabled(false);
                resumeButton.setEnabled(resumeCursor != null && !isDestroyed());
            });
        }
    }

    private void startEdgeSearch() {
        final String four = fourInput.getText().toString().trim();
        final boolean firstHalf = halfGroup.getCheckedRadioButtonId()
                == halfGroup.getChildAt(0).getId();
        int parsedLimit;
        try { parsedLimit = Integer.parseInt(pageLimitInput.getText().toString().trim()); }
        catch (RuntimeException invalid) { parsedLimit = -1; }
        final int threadLimit = parsedLimit;
        if (!WacchoiSearch.validToken(four)) {
            fourInput.setError("英数字・+・/ の4文字を指定してください"); return;
        }
        if (threadLimit < 1 || threadLimit > MAX_LIST_PAGES) {
            pageLimitInput.setError("1〜1000を指定してください"); return;
        }
        if(startBackground(new Intent().putExtra("edge",true).putExtra("four",four)
                .putExtra("upper",firstHalf).putExtra("limit",threadLimit)))return;
        WacchoiSearchService.current=null;observedBackground=null;
        showingHistory=false;
        localHistory=WacchoiHistory.begin(this,new Intent().putExtra("edge",true).putExtra("four",four)
                .putExtra("upper",firstHalf).putExtra("limit",threadLimit));
        cancelled = false;
        resumeCursor = null;
        displayedResults.clear();
        resumeButton.setEnabled(false);
        startButton.setEnabled(false); edgeStartButton.setEnabled(false);
        stopButton.setEnabled(true); resultsLayout.removeAllViews();
        statusView.setText("エッヂの現行スレ一覧を取得しています。DATは最低10秒間隔で照合します。");
        job = executor.submit(() -> runEdgeSearch(four, firstHalf, threadLimit, null));
    }

    private void runEdgeSearch(String four, boolean firstHalf, int threadLimit, SearchCursor resume) {
        int requests = 0, scanned = 0, found = 0;
        try {
            List<String> ids = resume == null ? new ArrayList<>() : resume.edgeIds;
            if (resume == null) {
                String subject = fetchEdge(EDGE_SUBJECT); requests++;
                Matcher threads = EDGE_SUBJECT_ROW.matcher(subject);
                while (threads.find()) ids.add(threads.group(1));
            }
            if (ids.isEmpty()) {
                throw new IllegalStateException("現行スレ一覧からスレIDを読み取れませんでした");
            }
            int index = resume == null ? 0 : resume.edgeIndex;
            for (; index < ids.size(); index++) {
                String id = ids.get(index);
                resumeCursor = new SearchCursor(null, "", "", "", "", 0, four,
                        firstHalf, threadLimit, ids, index);
                if (cancelled || scanned >= threadLimit) break;
                String dat = fetchEdge(EDGE_DAT + id + ".dat"); requests++; scanned++;
                String[] lines = dat.split("\\r?\\n");
                for (int i = 0; i < lines.length; i++) {
                    String[] columns = lines[i].split("<>", -1);
                    String identity = columns.length > 0 ? columns[0] : "";
                    if (columns.length > 2) identity += " " + columns[2];
                    Matcher slip = SLIP.matcher(identity);
                    while (slip.find()) {
                        if ((firstHalf ? slip.group(1) : slip.group(2)).equals(four)) {
                            found++;
                            String threadUrl = "https://bbs.eddibb.cc/test/read.cgi/liveedge/"
                                    + id + "/" + (i + 1) + "/";
                            publishEdgeResult(threadUrl, "現行スレ " + id + " / "
                                    + (i + 1) + "レス", slip.group(), identity);
                            break;
                        }
                    }
                }
                publishStatus("エッヂ現行スレ " + scanned + "/" + Math.min(ids.size(), threadLimit)
                        + "件 / 通信 " + requests + "回 / 一致 " + found);
                resumeCursor = new SearchCursor(null, "", "", "", "", 0, four,
                        firstHalf, threadLimit, ids, index + 1);
            }
            if (index >= ids.size()) resumeCursor = null;
            String state = cancelled ? "停止しました" : scanned >= threadLimit && ids.size() > scanned
                    ? "スレ件数上限に達しました" : "現行スレの走査が完了しました";
            publishStatus(state + "。現行スレ " + scanned + "/" + ids.size()
                    + "件 / 通信 " + requests + "回 / 一致 " + found
                    + "。過去ログは対象外です。");
        } catch (Exception error) {
            publishStatus((cancelled ? "停止しました" : "エッヂ検索を中断しました") + ": "
                    + error.getMessage() + "。通信 " + requests + "回、照合 " + scanned + "スレ。");
        } finally {
            runOnUiThread(() -> {
                startButton.setEnabled(true); edgeStartButton.setEnabled(true);
                stopButton.setEnabled(false);
                resumeButton.setEnabled(resumeCursor != null && !isDestroyed());
            });
        }
    }

    private String fetchEdge(String address) throws Exception {
        long wait = MIN_INTERVAL_MS - (SystemClock.elapsedRealtime() - lastRequestAt);
        if (lastRequestAt != 0 && wait > 0) Thread.sleep(wait);
        if (cancelled || Thread.currentThread().isInterrupted())
            throw new InterruptedException("停止しました");
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        activeConnection = connection;
        connection.setConnectTimeout(20_000); connection.setReadTimeout(30_000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) Haiagaru/1.0");
        lastRequestAt = SystemClock.elapsedRealtime();
        try {
            int code = connection.getResponseCode();
            if (code == 403 || code == 429 || code >= 500)
                throw new IllegalStateException("エッヂ応答 " + code + "。負荷・制限のため停止しました");
            if (code < 200 || code >= 300) throw new IllegalStateException("エッヂ応答 " + code);
            byte[] bytes;
            try (InputStream in = connection.getInputStream()) { bytes = readBytes(in); }
            return new String(bytes, Charset.forName("Shift_JIS"));
        } finally { connection.disconnect(); activeConnection = null; }
    }

    private List<String> scanRows(String body, String base, String four, boolean firstHalf) {
        List<String> found = new ArrayList<>();
        Matcher rows = ROW.matcher(body);
        while (rows.find() && !cancelled) {
            String row = rows.group(1);
            String profileUrl = null;
            Matcher link = PROFILE_LINK.matcher(row);
            while (link.find()) {
                String href = link.group(1).replace("&amp;", "&");
                String path = href.split("[?#]", 2)[0];
                if (path.endsWith(".html")) {
                    profileUrl = WacchoiSearch.profileUrl(base, href);
                    if (profileUrl != null) break;
                }
            }
            if (profileUrl == null) continue;
            // Hissi has changed table columns over time. Match the full row instead of
            // assuming that the poster name/ID is always the final table cell.
            String names = Html.fromHtml(row, Html.FROM_HTML_MODE_LEGACY).toString()
                    .replace('\u00a0', ' ').replace('\u3000', ' ').trim();
            Matcher slip = SLIP.matcher(names);
            while (slip.find()) {
                if ((firstHalf ? slip.group(1) : slip.group(2)).equals(four)) {
                    found.add(profileUrl + "\n" + names + "\n" + slip.group());
                    break;
                }
            }
        }
        return found;
    }

    private String fetch(String address) throws Exception {
        File cache = cacheFile(address);
        if (WacchoiStorageLimits.fresh(this,cache)) {
            try (InputStream in = new FileInputStream(cache)) { return readText(in); }
        }
        long wait = MIN_INTERVAL_MS - (SystemClock.elapsedRealtime() - lastRequestAt);
        if (lastRequestAt != 0 && wait > 0) Thread.sleep(wait);
        if (cancelled || Thread.currentThread().isInterrupted()) throw new InterruptedException("停止しました");
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        activeConnection = connection;
        connection.setConnectTimeout(20_000); connection.setReadTimeout(20_000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) Haiagaru/1.0");
        connection.setInstanceFollowRedirects(true);
        lastRequestAt = SystemClock.elapsedRealtime();
        try {
            int code = connection.getResponseCode();
            if (code == 403 || code == 429 || code >= 500)
                throw new IllegalStateException("Hissi応答 " + code + "。サーバー負荷のため停止しました");
            if (code < 200 || code >= 300) throw new IllegalStateException("Hissi応答 " + code);
            byte[] bytes;
            try (InputStream in = connection.getInputStream()) { bytes = readBytes(in); }
            String text = new String(bytes, Charset.forName("Shift_JIS"));
            cache.setLastModified(System.currentTimeMillis());
            try (FileOutputStream out = new FileOutputStream(cache)) { out.write(bytes); }
            trimCache(cache.getParentFile());
            return text;
        } finally { connection.disconnect(); activeConnection = null; }
    }

    private String readText(InputStream in) throws Exception {
        return new String(readBytes(in), Charset.forName("Shift_JIS"));
    }

    private byte[] readBytes(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192]; int total = 0, count;
        while ((count = in.read(buffer)) != -1) {
            total += count;
            if (total > RESPONSE_LIMIT) throw new IllegalStateException("応答サイズ上限を超えました");
            out.write(buffer, 0, count);
        }
        return out.toByteArray();
    }

    private File cacheFile(String value) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));
        StringBuilder name = new StringBuilder();
        for (byte b : hash) name.append(String.format(Locale.US, "%02x", b));
        return WacchoiPageCache.file(this,name+".html");
    }

    private void trimCache(File dir) {
        long limit = WacchoiStorageLimits.scaled(this,5,1024L*1024);
        File[] files = dir.listFiles(); if (files == null) return;
        long size = 0; for (File file : files) size += file.length();
        if (size <= limit) return;
        java.util.Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        for (File file : files) { if (size <= limit) break; size -= file.length(); file.delete(); }
    }

    private void stopSearch() {
        if(WacchoiSearchService.current!=null && WacchoiSearchService.current.running){WacchoiSearchService.current.cancel();return;}
        cancelled = true;
        HttpURLConnection connection = activeConnection;
        if (connection != null) new Thread(connection::disconnect, "wacchoi-search-stop").start();
        Future<?> current = job; if (current != null) current.cancel(true);
        statusView.setText("停止中です…");
    }

    private void publishStatus(String value) { if(localHistory!=null&&!showingHistory)localHistory.status(value);runOnUiThread(() -> statusView.setText(value)); }
    private void publishResult(String item, String caption) {
        if(localHistory!=null&&!showingHistory)localHistory.add(0,item,caption,"","");
        String[] values = item.split("\\n", 3);
        runOnUiThread(() -> {
            if (isDestroyed() || !displayedResults.add(values[0])) return;
            TextView row = new TextView(this);
            row.setText(caption + "\n" + values[2] + "\n" + values[1] + "\n投稿者ページを開く");
            row.setTextSize(14); row.setPadding(dp(10), dp(9), dp(10), dp(9));
            row.setOnClickListener(v -> open(values[0]));
            resultsLayout.addView(row, row());
        });
    }
    private void publishThread(HissiThreadResults.Result result) {
        if(localHistory!=null&&!showingHistory)localHistory.add(1,result.url,result.titleHtml,result.bodyHtml,"");
        runOnUiThread(() -> {
            if (isDestroyed() || !displayedResults.add(result.url)) return;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(10), dp(12), dp(10), dp(12));
            TextView title = new TextView(this);
            String titleText = Html.fromHtml(result.titleHtml, Html.FROM_HTML_MODE_LEGACY).toString().trim();
            title.setText(titleText.isEmpty() ? "スレ名不明" : titleText);
            title.setTextSize(16); title.setTypeface(null, Typeface.BOLD);
            title.setTextIsSelectable(true);
            card.addView(title, row());
            TextView body = new TextView(this);
            String bodyText = Html.fromHtml(result.bodyHtml, Html.FROM_HTML_MODE_LEGACY).toString().trim();
            body.setText(bodyText.isEmpty() ? "この検索元には書き込み内容が掲載されていません" : bodyText);
            body.setTextSize(14); body.setTextIsSelectable(true);
            card.addView(body, row());
            Button button = new Button(this);
            button.setText("未取得スレを開く: " + result.url);
            button.setAllCaps(false); button.setOnClickListener(v -> open(result.url));
            card.addView(button, row());
            resultsLayout.addView(card, row());
        });
    }
    private void publishEdgeResult(String url, String caption, String slip, String identity) {
        if(localHistory!=null&&!showingHistory)localHistory.add(2,url,caption,slip,identity);
        runOnUiThread(() -> {
            if (isDestroyed() || !displayedResults.add(url)) return;
            TextView row = new TextView(this);
            row.setText(caption + "\n" + slip + "\n" + identity + "\n" + url);
            row.setTextSize(14); row.setPadding(dp(10), dp(9), dp(10), dp(9));
            row.setOnClickListener(v -> open(url));
            resultsLayout.addView(row, row());
        });
    }
    private void open(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception error) { Toast.makeText(this, "リンクを開けませんでした", Toast.LENGTH_SHORT).show(); }
    }
    private void openHistory(org.json.JSONObject saved){
        new android.app.AlertDialog.Builder(this).setTitle("検索履歴")
            .setItems(new String[]{"保存した結果を表示（通信なし）","同じ条件で再検索","保存した結果の中を検索（通信なし）"},(dialog,which)->{
                if((WacchoiSearchService.current!=null&&WacchoiSearchService.current.running)||(job!=null&&!job.isDone())){
                    Toast.makeText(this,"実行中の検索を停止してから履歴を開いてください",Toast.LENGTH_SHORT).show();return;
                }
                fourInput.setText(saved.optString("four"));halfGroup.check(halfGroup.getChildAt(saved.optBoolean("upper")?0:1).getId());
                fromInput.setText(saved.optString("from"));toInput.setText(saved.optString("to"));pageLimitInput.setText(String.valueOf(saved.optInt("limit",20)));
                selectedBoards.clear();String boards=saved.optString("boards");
                if("*".equals(boards))selectedBoards.addAll(availableBoardCodes());
                else for(String board:boards.split(","))if(!board.isEmpty())selectedBoards.add(board);
                renderBoardChoices();
                if(which==1){if(saved.optBoolean("edge"))startEdgeSearch();else startSearch();return;}
                if(which==2){
                    EditText query=new EditText(this);query.setSingleLine(true);query.setHint("ID・スレ名・本文・URL（空白区切りAND）");
                    new android.app.AlertDialog.Builder(this).setTitle("保存結果内検索").setView(query)
                        .setNegativeButton("キャンセル",null).setPositiveButton("検索",(d,i)->showSavedHistory(saved,query.getText().toString())).show();return;
                }
                showSavedHistory(saved,"");
            }).setNegativeButton("閉じる",null).show();
    }
    private void showSavedHistory(org.json.JSONObject saved,String query){
                if((WacchoiSearchService.current!=null&&WacchoiSearchService.current.running)||(job!=null&&!job.isDone())){
                    Toast.makeText(this,"実行中の検索を停止してから履歴を開いてください",Toast.LENGTH_SHORT).show();return;
                }
                showingHistory=true;localHistory=null;displayedResults.clear();resultsLayout.removeAllViews();resumeCursor=null;
                org.json.JSONArray results=saved.optJSONArray("results");
                int matched=0;
                if(results!=null)for(int i=0;i<results.length();i++){
                    org.json.JSONObject r=results.optJSONObject(i);if(r==null)continue;
                    if(!WacchoiHistorySearch.matches(WacchoiHistory.recordText(r),query))continue;
                    matched++;
                    if(r.optInt("kind")==0)publishResult(r.optString("a"),r.optString("b"));
                    else if(r.optInt("kind")==1)publishThread(new HissiThreadResults.Result(r.optString("a"),r.optString("b"),r.optString("c")));
                    else publishEdgeResult(r.optString("a"),r.optString("b"),r.optString("c"),r.optString("d"));
                }
                statusView.setText("保存結果: "+saved.optString("status")+(query.trim().isEmpty()?"":" / 「"+query+"」該当"+matched+"件")+(saved.optBoolean("truncated")?"（保存容量の上限により一部のみ）":""));
                startButton.setEnabled(true);edgeStartButton.setEnabled(true);stopButton.setEnabled(false);resumeButton.setEnabled(false);
    }

    /** Keep the service's JavaScript-rendered search results inside the app. */
    private void openZzzSearch(String url) {
        if (zzzDialog != null) zzzDialog.dismiss();
        android.app.Dialog dialog = new android.app.Dialog(this,
                android.R.style.Theme_Material_Light_NoActionBar);
        zzzDialog = dialog;
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        LinearLayout actions = new LinearLayout(this);
        Button back = new Button(this); back.setText("戻る");
        Button external = new Button(this); external.setText("外部で開く");
        actions.addView(back, new LinearLayout.LayoutParams(0, dp(48), 1));
        actions.addView(external, new LinearLayout.LayoutParams(0, dp(48), 1));
        panel.addView(actions);
        android.widget.ProgressBar loading = new android.widget.ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        panel.addView(loading, new LinearLayout.LayoutParams(-1, dp(3)));
        android.webkit.WebView web = new android.webkit.WebView(this);
        zzzWebView = web;
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setSupportMultipleWindows(false);
        web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override public void onProgressChanged(android.webkit.WebView view, int progress) {
                loading.setProgress(progress);
                loading.setVisibility(progress == 100 ? View.GONE : View.VISIBLE);
            }
        });
        web.setWebViewClient(new android.webkit.WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(android.webkit.WebView view, String target) {
                Uri uri = Uri.parse(target);
                if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())))
                    return true;
                String path = uri.getPath();
                if (path != null && (path.contains("/test/read.cgi/") || path.contains("/bbs/read.cgi/"))) {
                    open(target);
                    return true;
                }
                return false;
            }
        });
        panel.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        back.setOnClickListener(v -> { if (web.canGoBack()) web.goBack(); else dialog.dismiss(); });
        external.setOnClickListener(v -> open(web.getUrl() == null ? url : web.getUrl()));
        dialog.setOnKeyListener((d, key, event) -> {
            if (key != android.view.KeyEvent.KEYCODE_BACK) return false;
            if (event.getAction() == android.view.KeyEvent.ACTION_UP) {
                if (web.canGoBack()) web.goBack(); else dialog.dismiss();
            }
            return true;
        });
        dialog.setOnDismissListener(d -> {
            web.stopLoading(); web.setWebChromeClient(null); web.setWebViewClient(null);
            panel.removeView(web); web.destroy();
            if (zzzWebView == web) { zzzWebView = null; zzzDialog = null; }
        });
        dialog.setContentView(panel);
        dialog.show();
        dialog.getWindow().setLayout(-1, -1);
        web.loadUrl(url);
    }

    public static void openFromToolbar(Activity activity, boolean upper) {
        Intent intent = new Intent(activity, HissiWacchoiSearchActivity.class);
        if (upper) intent.putExtra(EXTRA_HALF, HALF_UPPER);
        activity.startActivity(intent);
    }

    private void resumeSearch() {
        if(WacchoiSearchService.current!=null && WacchoiSearchService.current.canResume()){
            WacchoiSearchService.launch(this,new Intent().setAction("resume"));return;
        }
        SearchCursor cursor = resumeCursor;
        if (cursor == null) return;
        cancelled = false;
        startButton.setEnabled(false); edgeStartButton.setEnabled(false);
        resumeButton.setEnabled(false); stopButton.setEnabled(true);
        statusView.setText("前回の続きから検索しています。");
        job = executor.submit(() -> {
            if (cursor.edgeIds != null) runEdgeSearch(cursor.four, cursor.firstHalf, cursor.limit, cursor);
            else runSearch(cursor.boardChoice, cursor.date, cursor.to, cursor.four,
                    cursor.firstHalf, cursor.limit, cursor);
        });
    }

    private static final class SearchCursor {
        final List<String> boards, edgeIds;
        final String boardChoice, date, to, board, four;
        final int page, limit, edgeIndex;
        final boolean firstHalf;

        SearchCursor(List<String> boards, String boardChoice, String date, String to, String board,
                int page, String four, boolean firstHalf, int limit, List<String> edgeIds, int edgeIndex) {
            this.boards = boards; this.boardChoice = boardChoice; this.date = date; this.to = to;
            this.board = board; this.page = page; this.four = four; this.firstHalf = firstHalf;
            this.limit = limit; this.edgeIds = edgeIds; this.edgeIndex = edgeIndex;
        }
    }

    private static final class BoardEntry {
        final String code;
        final String label;
        BoardEntry(String code, String label) { this.code = code; this.label = label; }
    }

    private TextView label(String value) { TextView view = new TextView(this); view.setText(value); return view; }
    private EditText field(String hint, String value) {
        EditText edit = new EditText(this); edit.setSingleLine(true); edit.setHint(hint); edit.setText(value); return edit;
    }
    private LinearLayout.LayoutParams row() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }
    private String today() { return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date()); }
    private boolean validDate(String date) {
        if (!date.matches("[0-9]{8}")) return false;
        try { SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.US); format.setLenient(false); format.parse(date); return true; }
        catch (Exception invalid) { return false; }
    }
    private int daysBetween(String from, String to) {
        try {
            Calendar a = Calendar.getInstance(), b = Calendar.getInstance();
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.US); format.setLenient(false);
            a.setTime(format.parse(from)); b.setTime(format.parse(to));
            return (int)((b.getTimeInMillis() - a.getTimeInMillis()) / TimeUnit.DAYS.toMillis(1));
        } catch (Exception error) { return Integer.MAX_VALUE; }
    }

    @Override protected void onDestroy() {
        backgroundUi.removeCallbacks(refreshBackground);
        if (zzzDialog != null) zzzDialog.dismiss();
        cancelled = true;
        HttpURLConnection connection = activeConnection;
        if (connection != null) new Thread(connection::disconnect, "wacchoi-search-close").start();
        Future<?> current = job; if (current != null) current.cancel(true);
        executor.shutdownNow();
        super.onDestroy();
    }
}
