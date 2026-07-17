#!/bin/bash
# Wrapper: convert MSYS/Unix paths in args to Windows paths before calling real java.exe
REAL_JAVA="/c/Users/18043/AppData/Local/Programs/Java/jdk-21/bin/java.exe"
out=()
for a in "$@"; do
  if [[ "$a" == -D* ]]; then
    key="${a%%=*}"
    val="${a#*=}"
    if [[ "$val" == /* || "$val" == [a-zA-Z]:* ]]; then
      cv=$(cygpath -w "$val" 2>/dev/null)
      [ -n "$cv" ] && val="$cv"
    fi
    out+=("$key=$val")
  elif [[ "$a" == /* ]]; then
    cv=$(cygpath -w "$a" 2>/dev/null)
    if [ -n "$cv" ]; then out+=("$cv"); else out+=("$a"); fi
  else
    out+=("$a")
  fi
done
exec "$REAL_JAVA" "${out[@]}"
