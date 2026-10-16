#!/bin/bash
# usage: ./m5_run.sh <label> <zones>   (stress Area already open on the canvas)
PKG=com.practice.plantUser
LABEL=$1; ZONES=$2
OUT=~/m5_results; mkdir -p "$OUT"

pan() { for i in $(seq 1 10); do
  adb shell input swipe 800 1200 300 1200 250
  adb shell input swipe 300 1200 800 1200 250
done; }

pan                                             # warm-up
adb shell dumpsys gfxinfo $PKG reset > /dev/null
adb logcat -c
pan                                             # measured run

f="$OUT/gfx_$LABEL.txt"
adb shell dumpsys gfxinfo $PKG > "$f"

# GC lines for this process only
pid=$(adb shell pidof $PKG | tr -d '\r')
adb logcat -d --pid=$pid | grep "GC freed" > "$OUT/gc_$LABEL.txt"
gcs=$(wc -l < "$OUT/gc_$LABEL.txt")
freed_kb=$(grep -o "freed [0-9]*KB" "$OUT/gc_$LABEL.txt" | tr -dc '0-9\n' | awk '{s+=$1} END{print s+0}')

frames=$(awk '/Total frames rendered/{print $4}' "$f")
janky=$(awk '/^Janky frames:/{print $3}' "$f")
jankyP=$(awk '/Janky frames:/{print $4}' "$f")
p50=$(awk '/^50th percentile/{gsub("ms","",$3); print $3}' "$f")
p90=$(awk '/^90th percentile/{gsub("ms","",$3); print $3}' "$f")
p99=$(awk '/^99th percentile/{gsub("ms","",$3); print $3}' "$f")
missed=$(awk '/Number Missed Vsync/{print $4}' "$f")
inputlat=$(awk '/Number High input latency/{print $5}' "$f")
slowui=$(awk '/Number Slow UI thread/{print $5}' "$f")
drawslow=$(awk '/Number Slow issue draw commands/{print $6}' "$f")
rendernode=$(awk '/Total RenderNode/{print $4}' "$f")
gcs=$(grep -c "GC freed" "$OUT/gc_$LABEL.txt")

csv="$OUT/m5.csv"
[ -f "$csv" ] || echo "label,zones,frames,janky,jankyP,p50,p90,p99,missed,inputlat,slowui,drawslow,rendernode,gc_count" > "$csv"
echo "$LABEL,$ZONES,$frames,$janky,$jankyP,$p50,$p90,$p99,$missed,$inputlat,$slowui,$drawslow,$rendernode,$gcs" >> "$csv"
