package info.henrycaldwell.streamline.download;

import java.nio.file.Files;
import java.nio.file.Path;

public final class WriteFileProcess {

  private WriteFileProcess() {
  }

  public static void main(String[] args) throws Exception {
    Files.writeString(Path.of(args[0]), "content");
  }
}
