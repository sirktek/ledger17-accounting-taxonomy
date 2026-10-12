
## Handoff: Group 1
Changed: pom.xml, mvnw, mvnw.cmd, .mvn/, .gitignore, LICENSE.md, README.md (stub), .github/workflows/{build_only,build_tag_and_release}.yml,
src/main/java/com/ledger17/taxonomy/accounting/package-info.java, src/main/resources/ontologies/.gitkeep, OntologySyntaxTest.
Verified: `mvnq -q verify` green (OntologySyntaxTest passes with zero .ttl files); dependency:tree compile scope is Jena only.
Notes: build_only.yml copied verbatim from the template (runs PMD `pmd:check` as in the template). Release workflow publishes to GitHub Packages only, revision 1.${run_number}.
Unverified: CI workflows not run. Deviation: used aggregate `junit-jupiter` 5.11.3 instead of separate api/engine.
