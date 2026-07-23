#!/usr/bin/env bash
# PreToolUse(Bash) reminder: nudge toward the git-workflow skill before a
# branch-creating git command runs. Soft reminder only - never blocks.
set -euo pipefail

command=$(jq -r '.tool_input.command // empty')

if echo "$command" | grep -qE 'git +checkout +-b +[^ ]|git +switch +-c +[^ ]|git +branch +[^ -]'; then
  cat <<'EOF'
{"hookSpecificOutput":{"hookEventName":"PreToolUse","additionalContext":"Reminder: before creating this branch, consult the affaan-m-everything-claude-code-git-workflow skill for this repo's branching/commit conventions."}}
EOF
fi
