#!groovy
/*
 * VisualSync - a tool to visualize user data synchronization
 * Copyright (c) 2016 Gary Clayburg
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
def starttime = System.currentTimeMillis()
stage "provision build node"
node('coreosnode') {  //this node label must match jenkins slave with nodejs installed
    println("begin: build node ready in ${(System.currentTimeMillis() - starttime) / 1000}  seconds")
    wrap([$class: 'TimestamperBuildWrapper']) {  //wrap each Jenkins job console output line with timestamp
        stage "build setup"
        checkout scm
        whereami()

        stage "build/test"
        try {
            sh "./gradlew --no-daemon clean build buildImage pushVersion pushLatest --info"
        } catch(err) {
            throw err
        } finally {
            step([$class: 'JUnitResultArchiver', testResults: 'build/**/TEST-*.xml'])
            step([$class: 'WarningsPublisher', canComputeNew: false, canResolveRelativePaths: false, categoriesPattern: '', consoleParsers: [[parserName: 'asciidoctor-warning']], defaultEncoding: '', excludePattern: '', failedTotalAll: '1', healthy: '', includePattern: '', messagesPattern: '', unHealthy: '', unstableTotalAll: '0'])
            archive('build/libs/**/*.jar')
        }

        stage "docker"
        sh "pwd && ls "
//        sh "mvn docker:build -DpushImage"
/*
i.e. match this:
12:31:12 [qbb_fastWarDockerBranch] asciidoctor: WARNING: api-guide.adoc: line 380: no callouts refer to list item 1

asciidoctor: WARNING: api-guide.adoc: line 457: include file not found: /home/jenkins/workspace/visualsync/service-core/target/generated-snippets/createone/curl-request.adoc
*/

        stage "archive"
//        step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
        step([$class: 'JUnitResultArchiver', testResults: 'build/**/TEST-*.xml'])

        println "flow complete!"
    }
}
private void whereami() {
    /**
     * Runs a bunch of tools that we assume are installed on this node
     */
    echo "Build is running with these settings:"
    sh "pwd"
    sh "ls -la"
    sh "echo path is \$PATH"
    sh """
uname -a
java -version
mvn -v
docker ps
docker info
#docker-compose -f src/main/docker/app.yml ps
docker-compose version
npm version
gulp --version
bower --version
"""
}
