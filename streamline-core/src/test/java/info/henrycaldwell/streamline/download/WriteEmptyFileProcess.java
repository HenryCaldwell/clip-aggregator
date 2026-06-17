package info.henrycaldwell.streamline.download;

import java.nio.file.Files;
import java.nio.file.Path;

public final class WriteEmptyFileProcess {

  private WriteEmptyFileProcess() {
  }

  public static void main(String[] args) throws Exception {
    Files.writeString(Path.of(args[0]), "");
  }
}
