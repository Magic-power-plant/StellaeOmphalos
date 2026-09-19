package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import java.util.List;

public record PktMigrationReport(String reportId, List<String> issues) implements OmphalosPayload {
    public static final Codec<PktMigrationReport> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("report_id").forGetter(PktMigrationReport::reportId),
            Codec.STRING.listOf().fieldOf("issues").forGetter(PktMigrationReport::issues)).apply(instance, PktMigrationReport::new));
    public PktMigrationReport {
        if (reportId.length() > 256 || issues.size() > 256 || issues.stream().anyMatch(issue -> issue.length() > 4096)) throw new IllegalArgumentException("Migration report too large");
        issues = List.copyOf(issues);
    }
}
