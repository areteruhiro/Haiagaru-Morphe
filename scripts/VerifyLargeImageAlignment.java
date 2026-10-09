import app.morphe.extension.chmate.LargeImageAlignment;

public final class VerifyLargeImageAlignment {
    private static void near(float actual, float expected) {
        if (Math.abs(actual - expected) > 0.01f) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        // 12000x8000 content, 1200x800 draft, displayed at 5%: old centre is ten times too far.
        near(LargeImageAlignment.offset(12000, .5f, .05f), -2700);
        near(LargeImageAlignment.offset(8000, .5f, .05f), -1800);
        near(12000 * .5f / 2 + LargeImageAlignment.offset(12000, .5f, .05f), 300);
        near(8000 * .5f / 2 + LargeImageAlignment.offset(8000, .5f, .05f), 200);
        // Rotated content swaps width/height; normal/full-resolution path is unchanged.
        near(LargeImageAlignment.offset(8000, .5f, .05f), -1800);
        near(LargeImageAlignment.offset(12000, .05f, .05f), 0);
        near(LargeImageAlignment.offset(12000, 2, 2), 0);
        // Reported 1257x16384 image and a 64x835 reduced preview.
        float display = 2200f / 16384f;
        float draft = display * 1257f / 64f;
        near(1257f * draft / 2 + LargeImageAlignment.offset(1257, draft, display), 1257 * display / 2);
        near(16384f * draft / 2 + LargeImageAlignment.offset(16384, draft, display), 1100);
        near(LargeImageAlignment.offset(0, .5f, .05f), 0);
        near(LargeImageAlignment.offset(12000, Float.NaN, .05f), 0);
        near(LargeImageAlignment.offset(12000, .5f, Float.POSITIVE_INFINITY), 0);
        System.out.println("PASS: draft centre, rotation, original bitmap, invalid dimensions/scales");
    }
}
