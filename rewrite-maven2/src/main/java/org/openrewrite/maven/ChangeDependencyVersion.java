/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven;

import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.AddToTag;
import org.openrewrite.xml.ChangeTagValue;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

import static org.openrewrite.Validated.required;

public class ChangeDependencyVersion extends MavenRefactorVisitor {
    private static final XPathMatcher dependencyMatcher = new XPathMatcher("/project/dependencies/dependency");

    private String groupId;

    @Nullable
    private String artifactId;

    private String toVersion;

    public ChangeDependencyVersion() {
        setCursoringOn();
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(@Nullable String artifactId) {
        this.artifactId = artifactId;
    }

    public void setToVersion(String toVersion) {
        this.toVersion = toVersion;
    }

    @Override
    public Validated validate() {
        return required("groupId", groupId)
                .and(required("artifactId", artifactId))
                .and(required("toVersion", toVersion));
    }

    @Override
    public Xml visitTag(Xml.Tag tag) {
        if (dependencyMatcher.matches(getCursor())) {
            if (groupId.equals(tag.getChildValue("groupId").orElse(model.getGroupId())) &&
                    tag.getChildValue("artifactId")
                            .map(a -> a.equals(artifactId))
                            .orElse(artifactId == null)) {
                Optional<Xml.Tag> versionTag = tag.getChild("version");
                if (versionTag.isPresent()) {
                    String version = versionTag.get().getValue().orElse(null);
                    if (version == null) {
                        // TODO change in dependency management...
                    } else if (version.contains("${") &&
                            !toVersion.equals(model.getProperty(version))) {
                        ChangePropertyValue changePropertyValue = new ChangePropertyValue();
                        changePropertyValue.setKey(version);
                        changePropertyValue.setToValue(toVersion);
                        andThen(changePropertyValue);
                    } else if (!toVersion.equals(version)) {
                        andThen(new ChangeTagValue.Scoped(versionTag.get(), toVersion));
                    }
                } else {
                    andThen(new AddToTag.Scoped(tag, "<version>" + toVersion + "</version>"));
                }
            }
        }

        return super.visitTag(tag);
    }
}
