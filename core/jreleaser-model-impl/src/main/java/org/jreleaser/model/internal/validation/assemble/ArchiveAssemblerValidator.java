/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2023 The JReleaser authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jreleaser.model.internal.validation.assemble;

import org.jreleaser.bundle.RB;
import org.jreleaser.model.Archive;
import org.jreleaser.model.Distribution;
import org.jreleaser.model.api.JReleaserContext.Mode;
import org.jreleaser.model.internal.JReleaserContext;
import org.jreleaser.model.internal.assemble.ArchiveAssembler;
import org.jreleaser.util.Errors;

import java.util.Map;

import static org.jreleaser.model.internal.validation.assemble.AssemblersValidator.validateAssembler;
import static org.jreleaser.model.internal.validation.common.Validator.resolveActivatable;
import static org.jreleaser.util.CollectionUtils.listOf;
import static org.jreleaser.util.StringUtils.isBlank;

/**
 * @author Andres Almiray
 * @since 0.8.0
 */
public final class ArchiveAssemblerValidator {
    private ArchiveAssemblerValidator() {
        // noop
    }

    public static void validateArchive(JReleaserContext context, Mode mode, Errors errors) {
        Map<String, ArchiveAssembler> archive = context.getModel().getAssemble().getArchive();
        if (!archive.isEmpty()) context.getLogger().debug("assemble.archive");

        for (Map.Entry<String, ArchiveAssembler> e : archive.entrySet()) {
            e.getValue().setName(e.getKey());
            if (mode.validateConfig() || mode.validateAssembly()) {
                validateArchive(context, mode, e.getValue(), errors);
            }
        }
    }

    private static void validateArchive(JReleaserContext context, Mode mode, ArchiveAssembler archive, Errors errors) {
        context.getLogger().debug("assemble.archive.{}", archive.getName());

        resolveActivatable(context, archive,
            listOf("assemble.archive." + archive.getName(), "assemble.archive"),
            "NEVER");
        if (!archive.resolveEnabled(context.getModel().getProject())) {
            context.getLogger().debug(RB.$("validation.disabled"));
            return;
        }

        if (isBlank(archive.getName())) {
            errors.configuration(RB.$("validation_must_not_be_blank", "archive.name"));
            context.getLogger().debug(RB.$("validation.disabled.error"));
            archive.disable();
            return;
        }

        archive.setPlatform(archive.getPlatform().mergeValues(context.getModel().getPlatform()));

        if (null == archive.getDistributionType()) {
            archive.setDistributionType(Distribution.DistributionType.BINARY);
        }

        if (isBlank(archive.getArchiveName())) {
            archive.setArchiveName("{{distributionName}}-{{projectVersion}}");
        }

        if (archive.getFormats().isEmpty()) {
            archive.addFormat(Archive.Format.ZIP);
        }

        if (archive.getFileSets().isEmpty()) {
            errors.configuration(RB.$("validation_archive_empty_fileset", archive.getName()));
        }

        validateAssembler(context, mode, archive, errors);
    }
}
