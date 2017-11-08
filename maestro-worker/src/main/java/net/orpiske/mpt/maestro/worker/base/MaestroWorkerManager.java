package net.orpiske.mpt.maestro.worker.base;

import net.orpiske.mpt.common.exceptions.MaestroConnectionException;
import net.orpiske.mpt.maestro.client.AbstractMaestroPeer;
import net.orpiske.mpt.maestro.notes.MaestroNote;
import net.orpiske.mpt.maestro.notes.PingRequest;
import net.orpiske.mpt.maestro.notes.StartSender;
import net.orpiske.mpt.maestro.notes.StatsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class MaestroWorkerManager extends AbstractMaestroPeer {
    private static final Logger logger = LoggerFactory.getLogger(MaestroWorkerManager.class);

    public MaestroWorkerManager(final String url, final String clientName) throws MaestroConnectionException {
        super(url, clientName);
    }


    protected void messageArrived(MaestroNote note) {
        logger.debug("Some message arrived");

        if (note instanceof PingRequest) {
            maestroMessageArrived((PingRequest) note);
        }
        if (note instanceof StatsRequest) {
            maestroMessageArrived((StatsRequest) note);
        }
    }


    protected void maestroMessageArrived(StatsRequest note) {
        logger.debug("Stats request received");
    }

    protected void maestroMessageArrived(PingRequest note) {
        logger.debug("Creation seconds.micro: {}.{}", note.getSec(), note.getUsec());

        Instant creation = Instant.ofEpochSecond(note.getSec(), note.getUsec() * 1000);
        Instant now = Instant.now();

        Duration d = Duration.between(creation, now);

        logger.debug("Elapsed: {} ", d.getNano() / 1000);
    }
}
