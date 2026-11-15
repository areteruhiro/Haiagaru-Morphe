"""Build the optional Morphe input ZIP from pinned upstream releases."""
import argparse
from pathlib import Path
from tempfile import TemporaryDirectory
import shutil
import urllib.request
import zipfile

MODEL = "https://huggingface.co/OwenElliott/image-safety-classifier-xs/resolve/54f4560/"
ORT = "https://raw.githubusercontent.com/microsoft/onnxruntime/v1.23.2/"
AAR = ("https://repo.maven.apache.org/maven2/com/microsoft/onnxruntime/"
       "onnxruntime-android/1.23.2/onnxruntime-android-1.23.2.aar")
ASSETS = "assets/haiagaru/image-moderation/"
NG_191 = Path(__file__).resolve().parents[1] / "optional-assets" / "haiagaru-image-ng.zip"


def download(url, destination):
    try:
        with urllib.request.urlopen(url, timeout=60) as source, destination.open("wb") as out:
            shutil.copyfileobj(source, out)
    except OSError as error:
        raise OSError(f"Could not fetch {url}: {error}") from error


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    parser.add_argument("--split", action="store_true", help="Also create separately selectable model and runtime ZIPs.")
    args = parser.parse_args()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with TemporaryDirectory() as directory:
        temp = Path(directory)
        urls = {
            "model.onnx": MODEL + "onnx/image-safety-classifier-xs.onnx",
            "MODEL-NOTICE.txt": MODEL + "README.md",
            "ONNX-LICENSE.txt": ORT + "LICENSE",
            "ONNX-ThirdPartyNotices.txt": ORT + "ThirdPartyNotices.txt",
            "SWIFTFORMER-LICENSE.txt": "https://raw.githubusercontent.com/Amshaker/SwiftFormer/main/LICENSE",
        }
        for name, url in urls.items():
            download(url, temp / name)
        download(AAR, temp / "runtime.aar")
        with zipfile.ZipFile(NG_191) as legacy, zipfile.ZipFile(temp / "runtime.aar") as runtime, zipfile.ZipFile(
                args.output, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as bundle:
            for name in urls:
                entry = zipfile.ZipInfo(ASSETS + name)
                bundle.writestr(entry, (temp / name).read_bytes(), compress_type=zipfile.ZIP_DEFLATED)
            for abi in ("arm64-v8a", "armeabi-v7a", "x86", "x86_64"):
                fix = f"{abi}/libtensorflowlite_jni.so"
                bundle.writestr(zipfile.ZipInfo("chmate191/" + fix), legacy.read(fix), compress_type=zipfile.ZIP_DEFLATED)
                for name in ("libonnxruntime.so", "libonnxruntime4j_jni.so"):
                    suffix = f"{abi}/{name}"
                    bundle.writestr(zipfile.ZipInfo("lib/" + suffix), runtime.read("jni/" + suffix), compress_type=zipfile.ZIP_DEFLATED)
    if args.split:
        with zipfile.ZipFile(args.output) as combined:
            for part in ("model", "runtime"):
                target = args.output.with_name(args.output.stem + "-" + part + ".zip")
                with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as archive:
                    for name in combined.namelist():
                        is_model = name.endswith(("/model.onnx", "/MODEL-NOTICE.txt", "/SWIFTFORMER-LICENSE.txt"))
                        if name.startswith("chmate191/") or (part == "model") != is_model:
                            continue
                        archive.writestr(zipfile.ZipInfo(name), combined.read(name), compress_type=zipfile.ZIP_DEFLATED)


if __name__ == "__main__":
    main()
