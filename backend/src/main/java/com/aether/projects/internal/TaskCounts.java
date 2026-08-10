package com.aether.projects.internal;

/** How many tasks a project has in each state — enough to render list progress. */
record TaskCounts(int todo, int doing, int done) {

    static TaskCounts of(long todo, long doing, long done) {
        return new TaskCounts((int) todo, (int) doing, (int) done);
    }
}
