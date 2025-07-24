run:
	@./mvnw clean spring-boot:run

test:
	@./mvnw clean install

generate:
	@curl -X POST http://localhost:8080/generator \
		-F "name=foo" \
		-F "files=@fixtures/index.html" \
		-F "files=@fixtures/info.html" | jq

delete:
	@curl -X DELETE http://localhost:8080/generator \
		-F "name=foo"
