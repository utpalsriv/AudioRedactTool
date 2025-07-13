# Makefile for Java Stateless Example App
# Provides convenient commands for building, testing, and running the application

.PHONY: help test test-suite test-controller test-utils test-models test-health test-app clean build package run install coverage verify compile

# Default target
help:
	@echo "Available targets:"
	@echo "  help          - Show this help message"
	@echo "  test          - Run all unit tests"

# Test targets
test:
	@echo "Running all unit tests..."
	mvn test jacoco:report