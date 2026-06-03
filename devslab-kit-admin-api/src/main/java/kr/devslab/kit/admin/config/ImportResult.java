package kr.devslab.kit.admin.config;

import java.util.List;

/**
 * Outcome of a {@link ConfigImportService} run — the diff of what was (or, in dry-run,
 * would be) created/updated/deleted per type. Codes only; never UUIDs (ADR 0003).
 *
 * <p>{@code deleted} is only ever non-empty in {@code mirror} mode. {@code skipped} reports
 * entities that were intentionally left untouched — a role still assigned to users (mirror
 * refuses to strip it) or a user that already exists (import never overwrites users).
 *
 * <p>{@code users} is empty unless the run requested user sync ({@code includeUsers=true}).
 */
public record ImportResult(
        boolean dryRun,
        String mode,
        Section permissions,
        Section roles,
        Section menus,
        Section users
) {

    public record Section(List<String> created, List<String> updated, List<String> deleted, List<String> skipped) {

        public static final Section EMPTY = new Section(List.of(), List.of(), List.of(), List.of());

        public Section {
            created = List.copyOf(created);
            updated = List.copyOf(updated);
            deleted = List.copyOf(deleted);
            skipped = List.copyOf(skipped);
        }

        public static Section of(List<String> created, List<String> updated) {
            return new Section(created, updated, List.of(), List.of());
        }

        public static Section of(
                List<String> created, List<String> updated, List<String> deleted, List<String> skipped) {
            return new Section(created, updated, deleted, skipped);
        }
    }
}
