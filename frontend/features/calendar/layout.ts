import type { EventResponse } from "@/lib/api";

const DAY_MS = 24 * 60 * 60 * 1000;

export type PlacedEvent = {
  event: EventResponse;
  top: number; // fraction of the day [0, 1)
  height: number; // fraction of the day
  lane: number;
  laneCount: number;
};

type Segment = { event: EventResponse; start: number; end: number; lane: number };

/**
 * Positions a day's events within its column. Events are clipped to the day
 * (so overnight/multi-day events show in each day they touch), then split into
 * clusters of mutually-overlapping events; within a cluster each event gets a
 * lane so overlaps sit side by side.
 */
export function layoutDay(events: EventResponse[], dayStart: Date): PlacedEvent[] {
  const dayStartMs = dayStart.getTime();
  const dayEndMs = dayStartMs + DAY_MS;

  const segments: Segment[] = events
    .map((event) => ({
      event,
      start: Math.max(new Date(event.startsAt).getTime(), dayStartMs),
      end: Math.min(new Date(event.endsAt).getTime(), dayEndMs),
      lane: 0,
    }))
    .filter((segment) => segment.end > segment.start)
    .sort((a, b) => a.start - b.start || a.end - b.end);

  const placed: PlacedEvent[] = [];
  let cluster: Segment[] = [];
  let clusterEnd = -1;

  const flush = () => {
    const laneEnds: number[] = [];
    for (const segment of cluster) {
      let lane = laneEnds.findIndex((end) => segment.start >= end);
      if (lane === -1) {
        lane = laneEnds.length;
        laneEnds.push(segment.end);
      } else {
        laneEnds[lane] = segment.end;
      }
      segment.lane = lane;
    }
    for (const segment of cluster) {
      placed.push({
        event: segment.event,
        top: (segment.start - dayStartMs) / DAY_MS,
        height: (segment.end - segment.start) / DAY_MS,
        lane: segment.lane,
        laneCount: laneEnds.length,
      });
    }
    cluster = [];
    clusterEnd = -1;
  };

  for (const segment of segments) {
    if (cluster.length > 0 && segment.start >= clusterEnd) {
      flush();
    }
    cluster.push(segment);
    clusterEnd = Math.max(clusterEnd, segment.end);
  }
  if (cluster.length > 0) {
    flush();
  }

  return placed;
}
