# Running the application

Requires Java 25 and Maven. Build the JAR and its runtime dependencies:

```bash
mvn package
```

Pass the password directly on the command line, quoting the complete argument
with single quotes to preserve literal `$`, backslash, and `>` characters:

```bash
./run.sh 'dividendwatch.password=YOUR_PASSWORD' mode=DIVIDEND_WATCH stock.ticker=AAPL
```

The launcher defaults to `/usr/bin/chromium-browser`, debug logging enabled,
a visible browser, a 60-second timeout, and a 10-second post-login wait.
Set your username with `dividendwatch.username=your.email@example.com` or
`DIVIDENDWATCH_USERNAME`. Exported `DIVIDENDWATCH_BROWSER_EXECUTABLE_PATH`
(or `DIVIDENDWATCH_BROWSER_EXECUTABLE`), `DIVIDENDWATCH_DEBUG`,
`DIVIDENDWATCH_HEADLESS`, `DIVIDENDWATCH_PASSWORD`,
`DIVIDENDWATCH_POST_LOGIN_SETTLE_SECONDS`, `DIVIDENDWATCH_TIMEOUT_SECONDS`,
`DIVIDENDWATCH_USERNAME`, `MODE`, and `STOCK_TICKER` override these defaults.
The password has no default and is required for Dividend Watch. A command-line
password overrides `DIVIDENDWATCH_PASSWORD`; exporting the variable is optional.

To enter it literally, including characters such as `$`, backslashes, or `>`:

```bash
IFS= read -r -s -p 'Dividend Watch password: ' DIVIDENDWATCH_PASSWORD
printf '\n'
export DIVIDENDWATCH_PASSWORD
```

Avoid hardcoding a password in double quotes in the script: Bash expands `$`
expressions there. The launcher passes the exported value unchanged.

Arguments in `property=value` format override the environment and defaults:

```bash
./run.sh mode=EDGAR stock.ticker=MSFT 'edgar.user.agent=MyApp me@example.com'
./run.sh stock.ticker=AAPL dividendwatch.headless=true
```

The launcher converts properties to JVM `-D` options and executes `java -jar`.
It uses `JAVA_HOME/bin/java` when `JAVA_HOME` is set, otherwise `java` on `PATH`.
Keep `target/lib/` alongside the JAR; the JAR manifest references those dependencies.

## Missing values in balance-sheet output

Balance-sheet values are formatted numeric strings or JSON `null`:

- `"0"` represents a reported or calculated zero.
- `null` means the value is unavailable from the selected filing and cannot be
  calculated from the available inputs.

For example:

```json
{
  "metric": "Total equity",
  "values": {
    "2025": "0",
    "2024": null
  }
}
```

Here, total equity is zero for 2025 and unavailable for 2024.

Calculated metrics—including sums, differences, and residual “other”
categories—require all their inputs. If any required input is unavailable, the
calculated value is also `null`. An absent fact is never assumed to be zero.

Consumers should display `null` as **N/A** or **Unavailable** and preserve it
when processing data. Ratios or totals requiring a missing value should remain
unavailable rather than substituting zero.
