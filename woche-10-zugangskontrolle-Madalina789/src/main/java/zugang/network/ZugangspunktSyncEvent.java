package zugang.network;

import zugang.authinfo.Id;

public record ZugangspunktSyncEvent(Action action, Id id) {
}
