# simple Makefile which delegates all the real work to ant

ANT=ant
RM=rm

APP_NAME=BufferSizingGUI.jar
OBJ_DIR=build
BIN_DIR=dist
APP=$(BIN_DIR)/$(APP_NAME)


.PHONY=default clean run

default:
	$(ANT)

run:
	@if ! [ -f $(APP) ]; then                \
	  $(MAKE) --no-print-directory default;  \
	fi
	java -jar $(APP)

clean:
	$(RM) -rf $(OBJ_DIR) $(BIN_DIR)
