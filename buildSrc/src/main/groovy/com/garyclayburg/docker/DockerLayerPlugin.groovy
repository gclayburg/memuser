package com.garyclayburg.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

/**
 * <br><br>
 * Created 2017-09-03 17:36
 *
 * @author Gary Clayburg
 */
class DockerLayerPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def extension = project.extensions.create("dockerlayer", DockerLayerPluginExtension, project)
        def bootR = project.tasks.getByName('bootRepackage')
        def jartask = project.tasks.getByName('jar')

        project.task('copyDocker', type: Copy) {
            from extension.dockerSrcDirectoryProvider
            into extension.dockerBuildDirectoryProvider
        }
        project.task('copyClasses', type: Copy) {
            from project.zipTree(jartask.archivePath)
            into extension.dockerBuildClassesDirectoryProvider
            include "/BOOT-INF/classes/**"
            include "/META-INF/**"
        }.setDependsOn([bootR])
        project.task('copyDependencies', type: Copy) {
            from project.zipTree(jartask.archivePath)
            into extension.dockerBuildDependenciesDirectoryProvider
            exclude "/BOOT-INF/classes/**"
            exclude "/META-INF/**"
        }.setDependsOn([bootR])

        def dockerPrep = project.task('dockerPrepare')
        dockerPrep.dependsOn('copyClasses', 'copyDependencies', 'copyDocker')
    }
}
