IMG  := quay.io/geotrellis/comparative-analysis-query-server
TAG  := "nathan3"

ASSEMBLY_JAR := server/target/scala-2.11/query-server-assembly-*.jar

clean:
	rm -rf server/target/scala-2.11/query-server-assembly-0.0.1.jar
	rm ./build
	rm ./assemble

rwildcard=$(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2) $(filter $(subst *,%,$2),$d))

${ASSEMBLY_JAR}: $(call rwildcard, server, *.scala) build.sbt
	./sbt "project server" assembly

assembly: ${ASSEMBLY_JAR}

build: Dockerfile assembly
	docker build -t ${IMG}:${TAG}	.

publish: build
	docker push ${IMG}:${TAG}

test: build
	./sbt test
	docker-compose up -d
	sleep 2 && curl localhost:7070/system/status
	docker-compose down
