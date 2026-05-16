import re

controller_file = "src/main/java/io/github/dsheirer/module/decode/event/DecodeEventPanelController.java"
with open(controller_file, "r") as f:
    controller_content = f.read()

# remove SysCol, SiteCol
controller_content = re.sub(r'TableColumn<IDecodeEvent, String> sysCol = new TableColumn<>\("System"\);\n.*?sysCol.*?;\n', '', controller_content, flags=re.DOTALL)
controller_content = re.sub(r'TableColumn<IDecodeEvent, String> siteCol = new TableColumn<>\("Site"\);\n.*?siteCol.*?;\n', '', controller_content, flags=re.DOTALL)


# change from Time, Sys, Site... to Time, From, To
controller_content = controller_content.replace("tableView.getColumns().addAll(timeCol, sysCol, siteCol, fromCol, toCol, eventCol, durCol, protoCol, freqCol, chanCol);", "tableView.getColumns().addAll(timeCol, fromCol, toCol, eventCol, durCol, protoCol, freqCol, chanCol);")


with open(controller_file, "w") as f:
    f.write(controller_content)
