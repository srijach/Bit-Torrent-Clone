JC = javac
JFLAGS = -g
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	BitFieldMessage.java \
	CommonConfiguration.java \
  DataPiece.java \
	DataTransformUtils.java \
  DefaultMessageConfiguration.java \
	HandShake.java \
	Logger.java \
  MsgHandler.java \
  OrderByDataRate.java \
  PayLoad.java \
  PayLoadContainer.java \
  PeerManager.java \
	peerProcess.java \
  PeerStateInfo.java \
	RemotePeerInitiator.java \
  ThreadManager.java
default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class