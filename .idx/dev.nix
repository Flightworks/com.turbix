{ pkgs, ... }: {
  channel = "stable-24.05";
  packages = [
    pkgs.jdk17
    pkgs.android-tools
  ];
  env = {
    JAVA_HOME = "${pkgs.jdk17}/lib/openjdk";
    ANDROID_SDK_ROOT = "/home/user/.androidsdkroot";
  };
  idx = {
    extensions = [
      "vscjava.vscode-java-pack"
      "njpwerner.autodocstring"
      "ms-python.python"
    ];
    previews = {
      enable = true;
      previews = {
        android = {
          command = [
            "/bin/bash"
            "-c"
            "./gradlew assemblePlayDebug --no-daemon -Pandroid.injected.invoked.from.ide=true && mkdir -p app/build/outputs/apk/debug && cp app/build/outputs/apk/play/debug/app-play-debug.apk app/build/outputs/apk/debug/app-debug.apk"
          ];
          manager = "android";
        };
      };
    };
    workspace = {
      onCreate = {
        # Setup local.properties if it doesn't exist
        setup-sdk = "echo 'sdk.dir=/home/user/.androidsdkroot' > local.properties";
      };
      onStart = {
        # Any start commands
      };
    };
  };
}