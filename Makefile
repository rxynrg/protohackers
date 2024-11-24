GIT_SHA = $(shell git rev-parse --short HEAD)

BUILD_KIT_DEBUG_OPTS := --no-cache --progress=plain

EMPTY_CLASSPATH := [empty]

0_IMAGE_NAME := 0-smoke-test
0_MAIN_CLASS_NAME := Echo
0_CLASSPATH := $(EMPTY_CLASSPATH)

1_IMAGE_NAME := 1-prime-time
1_MAIN_CLASS_NAME := PrimeTime
1_CLASSPATH := json-20240303.jar

ALLOWED_IMAGES := 0 1

image-%:
	@if echo "$(ALLOWED_IMAGES)" | grep -qw "$*"; then \
		docker build \
			-t slick752/protohackers/$(value $*_IMAGE_NAME):$(GIT_SHA) \
			--build-arg APP_DIR=$(value $*_IMAGE_NAME) \
			--build-arg MAIN_CLASS_NAME=$(value $*_MAIN_CLASS_NAME) \
			--build-arg CLASSPATH=$(value $*_CLASSPATH) \
            -f Dockerfile \
            .; \
	else \
		echo "Error: image-$* is not an allowed target."; \
		exit 1; \
	fi
