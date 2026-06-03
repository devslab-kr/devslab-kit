package kr.devslab.kit.admin.config;

import java.util.List;

/**
 * Outcome of a {@link ConfigImportService} run — the diff of what was (or, in dry-run,
 * would be) created/updated per type. Codes only; never UUIDs (ADR 0003).
 */
public record ImportResult(
        boolean dryRun,
        String mode,
        Section permissions,
        Section roles,
        Section menus
) {

    public record Section(List<String> created, List<String> updated) {

        public static Section of(List<String> created, List<String> updated) {
            return new Section(List.copyOf(created), List.copyOf(updated));
        }
    }
}
