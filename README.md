# Streamline

Streamline is a configurable ETL pipeline designed to automate the full lifecycle of sourcing and publishing streaming clips to social media platforms.

## Prerequisites

- **Java (17+)**
- **Maven (3+)**
- **yt-dlp (latest version)** - Required if using the yt-dlp downloader
- **FFmpeg (latest version)** - Required if using any FFmpeg transformer

## Installation

1. **Clone the repository:**

```bash
git clone https://github.com/HenryCaldwell/streamline.git
cd streamline
```

2. **Build the project:**

```bash
mvn clean package
```

3. **Download required binaries (if applicable):**

- **yt-dlp** - Required if using the yt-dlp downloader. Download from [yt-dlp/releases](https://github.com/yt-dlp/yt-dlp/releases).
- **FFmpeg** - Required if using any FFmpeg transformer. Download from [ffmpeg.org](https://ffmpeg.org/download.html).

## Configuration

Streamline is driven entirely by a HOCON configuration file passed as a command-line argument at runtime. The file defines all components and how they connect.

A full annotated example covering every available option can be found in [`resources/example.conf`](resources/example.conf).

A minimal configuration is structured as follows:

```hocon
name    = "example"
posts   = 3
workDir = "/path/to/work"

# [optional] Exactly one. Records run and attempt events.
observer = {
  type = "sqlite",
  ...
}

# [required] One or more. Fetches candidate clips from a platform.
retrievers = [

  {
    type = "twitch",
    ...
  }

]

# [optional] Exactly one. Tracks clips to prevent duplicates.
history = {
  type = "sqlite",
  ...
}

# [required] Exactly one. Downloads clips for processing.
downloader = {
  type = "yt-dlp",
  ...
}

# [optional] One or more. Ordered transformer sequences applied to clips.
pipelines = [
  {
    name = "my_pipeline"

    transformers = [

      {
        type = "fps",
        ...
      }

    ]
  }
]

# [optional] Exactly one. Uploads processed clips to cloud storage.
stager = {
  type = "cloudflare-r2",
  ...
}

# [required] One or more. Delivers processed clips to social media platforms.
publishers = [

  {
    type = "instagram",
    ...
  }

]
```

## Usage

1. **Run with a configuration file:**

```bash
java -jar target/streamline-1.0.0.jar /path/to/config.conf
```

2. **Increase memory allocation for large runs:**

```bash
java -Xmx8g -Xms1g -jar target/streamline-1.0.0.jar /path/to/config.conf
```

3. **Troubleshooting:**

All errors follow the format `[CATEGORY:Component] Message (details)`, where `SPEC` indicates a configuration error and `COMPONENT` indicates a runtime error.

**Configuration (SPEC) errors:**

- `Invalid arguments` - Ensure exactly one config file path is passed as an argument.
- `Config file missing or not a regular file` - Verify the path points to an existing config file.
- `Missing required key` - An absent required field. Check [`resources/example.conf`](resources/example.conf) for reference.
- `Unknown X type` - An unrecognized type value. Check [`resources/example.conf`](resources/example.conf) for reference.
- `Invalid key value` - An invalid value. Check [`resources/example.conf`](resources/example.conf) for reference.

**Runtime (COMPONENT) errors:**

- **Observer errors** - Verify the database exists and is writable.
- **Retriever errors** - Verify your API credentials are valid and have the necessary permissions.
- **History errors** - Verify the database exists and is writable.
- **Downloader errors** - Verify the executable path is correct and the clip URL is accessible.
- **Transformer errors** - Verify the executable path is correct and the input media is valid.
- **Stager errors** - Verify your cloud storage credentials are correct, have the necessary permissions, and the bucket is accessible.
- **Publisher errors** - Verify your API credentials are valid, have the necessary permissions, and the media meets the platform's requirements.

## License

This project is licensed under the MIT License. See [`LICENSE`](LICENSE) for details.
