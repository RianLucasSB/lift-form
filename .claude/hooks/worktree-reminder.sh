#!/usr/bin/env bash
# WorktreeCreate reminder: nudge toward the git-workflow skill before a new
# worktree is created. Soft reminder only - never blocks.
set -euo pipefail

cat <<'EOF'
{"hookSpecificOutput":{"hookEventName":"WorktreeCreate","additionalContext":"Reminder: before creating this worktree, consult the affaan-m-everything-claude-code-git-workflow skill for this repo's branching/worktree conventions."}}
EOF
