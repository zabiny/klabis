#!/bin/bash

# Check if Gradle is currently running tests (excludes idle daemon threads)
# Detects both the ./gradlew driver process and GradleWorkerMain test executor
# processes (whose task name, e.g. "test", is not a standalone cmdline token).
# Exit codes: 0 = tests running, 1 = no tests running
#
# Usage:
#   ./checkGradleRunningTests.sh              # Show status and exit code
#   ./checkGradleRunningTests.sh -s           # Silent mode (only exit code)
#   ./checkGradleRunningTests.sh -v           # Verbose mode (show full commands)
#   ./checkGradleRunningTests.sh -k           # Kill all active test runs
#   ./checkGradleRunningTests.sh -d DIR       # Filter by working directory

show_help() {
    cat << EOF
Usage: checkGradleRunningTests.sh [OPTIONS]

Options:
  -s, --silent              Silent mode - only show exit code
  -v, --verbose             Verbose mode - show full Java commands
  -k, --kill                Kill all active test runs (keeps daemon)
  -d, --dir DIRECTORY       Filter by working directory (absolute path)
                            Can be used multiple times: -d /path/1 -d /path/2
  -h, --help                Show this help message

Exit codes:
  0 = Tests are running (matching filter, if given)
  1 = No tests running

Examples:
  ./checkGradleRunningTests.sh                                # Check status
  ./checkGradleRunningTests.sh -s                              # Silent mode
  ./checkGradleRunningTests.sh -d /path/to/project             # Check in specific directory
  if ./checkGradleRunningTests.sh -s -d /path/to/backend; then echo "Testing..."; fi
  ./checkGradleRunningTests.sh -k                               # Kill all test runs
  ./checkGradleRunningTests.sh -k -d /path                      # Kill test runs in specific dir
EOF
}

matches_dir_filter() {
    local process_cwd=$1
    shift
    local -a dir_filters=("$@")

    if [ ${#dir_filters[@]} -eq 0 ]; then
        return 0
    fi

    for dir_filter in "${dir_filters[@]}"; do
        if [[ "$process_cwd" == "$dir_filter"* ]]; then
            return 0
        fi
    done

    return 1
}

# A process is considered a "running test" process if:
#  - it's the gradlew/gradle driver invoked with a test-like task (test, check,
#    build, verify), OR
#  - it's a GradleWorkerMain whose cmdline shows it's executing tests: the
#    worker tmpdir/tmp path contains "/test/", or the trailing worker
#    display-name argument contains "Test Executor".
# GradleDaemon itself (idle daemon) is never considered a running test.
is_test_process() {
    local cmdline=$1

    if echo "$cmdline" | grep -q "GradleDaemon"; then
        return 1
    fi

    if echo "$cmdline" | grep -qE "(^|/)gradlew(\s|$)|(^|\s)gradle(\s|$)"; then
        if echo "$cmdline" | grep -qE "(^|\s)(test|check|build|verify)(\s|$)"; then
            return 0
        fi
        return 1
    fi

    if echo "$cmdline" | grep -q "GradleWorkerMain"; then
        if echo "$cmdline" | grep -qE "(/|\.)(test)(/|\.tmp|Executor)|Test Executor|/build/tmp/test/"; then
            return 0
        fi
        return 1
    fi

    return 1
}

check_running_tests() {
    local verbose=$1
    shift
    local -a dir_filters=("$@")

    local pgrep_output
    # Match only actual gradle/gradlew processes, not just filenames in jars/paths
    pgrep_output=$(pgrep -f "(^|/)gradlew\s|^gradle\s|GradleWorkerMain|GradleDaemon" 2>/dev/null)

    if [ -z "$pgrep_output" ]; then
        [ "$verbose" = "true" ] && echo "No Gradle processes found"
        return 1
    fi

    local test_processes=()
    local current_pid=$$

    while IFS= read -r pid; do
        if [ "$pid" -eq "$current_pid" ]; then
            continue
        fi

        local cmdline
        cmdline=$(cat "/proc/$pid/cmdline" 2>/dev/null | tr '\0' ' ')

        if echo "$cmdline" | grep -qE "checkGradleRunningTests|check_gradle_build"; then
            continue
        fi

        if ! is_test_process "$cmdline"; then
            continue
        fi

        if [ ${#dir_filters[@]} -gt 0 ]; then
            local process_cwd
            process_cwd=$(readlink -f "/proc/$pid/cwd" 2>/dev/null || echo "")
            if ! matches_dir_filter "$process_cwd" "${dir_filters[@]}"; then
                continue
            fi
        fi

        test_processes+=("$pid:$cmdline")
    done <<< "$pgrep_output"

    if [ ${#test_processes[@]} -eq 0 ]; then
        if [ "$verbose" = "true" ]; then
            local filter_str=""
            if [ ${#dir_filters[@]} -gt 0 ]; then
                local dirs_str=$(IFS=, ; echo "${dir_filters[*]}")
                filter_str="in '$dirs_str' "
            fi
            echo "✓ OK - No Gradle tests running ${filter_str}"
        fi
        return 1
    fi

    if [ "$verbose" = "true" ]; then
        local filter_str=""
        if [ ${#dir_filters[@]} -gt 0 ]; then
            local dirs_str=$(IFS=, ; echo "${dir_filters[*]}")
            filter_str="in '$dirs_str' "
        fi
        echo "⚠ Gradle tests running ${filter_str}:"
        echo ""

        for entry in "${test_processes[@]}"; do
            local pid="${entry%%:*}"
            local cmdline="${entry#*:}"
            local cwd=$(readlink -f "/proc/$pid/cwd" 2>/dev/null || echo "?")
            local kind="driver"
            echo "$cmdline" | grep -q "GradleWorkerMain" && kind="test worker"
            echo "  PID: $pid | Kind: $kind | CWD: $cwd"
        done
    fi

    return 0
}

kill_running_tests() {
    local -a dir_filters=("$@")

    local pgrep_output
    pgrep_output=$(pgrep -f "(^|/)gradlew\s|^gradle\s|GradleWorkerMain|GradleDaemon" 2>/dev/null)

    if [ -z "$pgrep_output" ]; then
        echo "No Gradle processes found"
        return 1
    fi

    local killed=0
    while IFS= read -r pid; do
        local cmdline
        cmdline=$(cat "/proc/$pid/cmdline" 2>/dev/null | tr '\0' ' ')

        if echo "$cmdline" | grep -qE "checkGradleRunningTests|check_gradle_build"; then
            continue
        fi

        if ! is_test_process "$cmdline"; then
            continue
        fi

        if [ ${#dir_filters[@]} -gt 0 ]; then
            local process_cwd
            process_cwd=$(readlink -f "/proc/$pid/cwd" 2>/dev/null || echo "")
            if ! matches_dir_filter "$process_cwd" "${dir_filters[@]}"; then
                continue
            fi
        fi

        echo "Killing test process PID: $pid"
        kill "$pid" 2>/dev/null && ((killed++))
    done <<< "$pgrep_output"

    if [ $killed -eq 0 ]; then
        echo "✓ OK - No active test runs to kill"
        return 1
    fi

    echo "✓ Killed $killed test process(es)"
    return 0
}

# Parse arguments
silent_mode=false
verbose_mode=false
kill_mode=false
dir_filters=()

while [[ $# -gt 0 ]]; do
    case $1 in
        -s|--silent)
            silent_mode=true
            shift
            ;;
        -v|--verbose)
            verbose_mode=true
            shift
            ;;
        -k|--kill)
            kill_mode=true
            shift
            ;;
        -d|--dir)
            if [[ -z "$2" ]]; then
                echo "Error: --dir requires a directory path argument"
                show_help
                exit 1
            fi
            dir_filters+=("$(cd "$2" 2>/dev/null && pwd || echo "$2")")
            shift 2
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

if [ "$kill_mode" = "true" ]; then
    kill_running_tests "${dir_filters[@]}"
else
    if [ "$silent_mode" = "true" ]; then
        check_running_tests false "${dir_filters[@]}"
    else
        check_running_tests true "${dir_filters[@]}"
    fi
fi
