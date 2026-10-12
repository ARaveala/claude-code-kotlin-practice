# Install List — Plant App Project

- [ ] Android Studio (includes Kotlin + Gradle + emulator/AVD manager)
- [ ] Java/JDK — bundled with Android Studio, no separate install needed
- [ ] Git — already installed (MariaDB work)
- [ ] Room (added as a Gradle dependency once project is scaffolded, not a system install)
- [ ] JUnit — bundled with Android Studio project templates by default

## Verify after install
- [ ] Android Studio opens and can create a new "Empty Activity" project
- [ ] Emulator (AVD) launches successfully and shows the default app
- [ ] Emulator gesture controls (pinch/rotate) checked in Extended Controls panel

## optional, recommended
- [ ] gitleaks, local secret scanning before pushing (CI also runs it )
- [ ] GitHub CLI(gh): open PR's and view CI results from the terminal
- [ ] claude code: only needed if using the assisted  AI workflow

## Setup steps

- [ ] `JAVA_HOME` set to Android Studio's bundled JBR, needed for command-line
      `./gradlew` builds. Linux example (adjust to where you unpacked it),
      in `~/.bashrc`:
      `export JAVA_HOME="$HOME/android-studio/jbr"`
      `export PATH="$JAVA_HOME/bin:$PATH"`
	  **user must confirm if true**
      Typical locations elsewhere: macOS
      `/Applications/Android Studio.app/Contents/jbr/Contents/Home`,
      Windows `C:\Program Files\Android\Android Studio\jbr`.
- [ ] Enable the commit-msg hook (hooks are not tracked by git): chmod +x .git/hooks/commit-msg (script in git_workflow.md)
- [ ] optional: enable EditorConfig support in android studio (settings-> Editor-> code style)

## verify
- [ ] `echo $JAVA_HOME` prints the JBR path and `java -version` runs
- [ ] `./gradlew build` succeeds from the project root
- [ ] `./gradlew ktlintCheck` runs and reports pass/fail without errors
- [ ] `gitleaks version` prints a version (if installed)
- [ ] `gh --version` prints a version (if installed)
- [ ] if gitleaks in use `gitleaks detect --source . -v` to make sure all is in order