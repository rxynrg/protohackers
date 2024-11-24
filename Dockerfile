FROM eclipse-temurin:23-jdk-noble AS builder
ARG APP_DIR
ARG MAIN_CLASS_NAME
ARG CLASSPATH
WORKDIR /build
COPY <<EOF manifest.mf
Main-Class: ${MAIN_CLASS_NAME}
EOF
COPY ${APP_DIR}/java/*.java .
COPY ${CLASSPATH} libs/
SHELL ["/bin/bash", "-c"]
RUN if [[ "$CLASSPATH" != "[empty]" ]]; then \
  javac -cp "libs/*" *.java; \
  (cd libs && ls | xargs -I {} bash -c \
    'jar xf {}; \
     find . -type f ! \( -name "*.class" -o -name "*.jar" \) -delete; \
     find . -type d -name "META-INF" | xargs rm -r'); \
  find libs -name "*.jar" -delete ;\
  jar cmvf manifest.mf fatapp.jar *.class -C libs/ .; \
else \
  javac *.java; \
  jar cmvf manifest.mf fatapp.jar *.class; fi

FROM eclipse-temurin:23-jre-noble AS runner
WORKDIR /app
COPY --from=builder /build/fatapp.jar app.jar
EXPOSE 8080/tcp
ENTRYPOINT ["java", \
    "-Djava.net.preferIPv6Stack=true", \
    "-jar", "app.jar"]
