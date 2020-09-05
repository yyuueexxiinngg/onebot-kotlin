ENV VERSION 0.2.3

RUN mkdir -p /app

ADD https://github.com/yyuueexxiinngg/cqhttp-mirai/releases/download/${VERSION}/cqhttp-mirai-${VERSION}-embedded-all.jar /app/cqhttp-mirai-embedded.jar

WORKDIR /app

ENTRYPOINT ["java", "-jar", "/app/cqhttp-mirai-embedded.jar"]
