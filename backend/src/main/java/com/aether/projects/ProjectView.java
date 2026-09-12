package com.aether.projects;

import java.util.UUID;

/** A project in list form, with how many tasks sit in each state. */
public record ProjectView(UUID id, String name, int todo, int doing, int done) {}
