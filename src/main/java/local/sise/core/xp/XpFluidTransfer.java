package local.sise.core.xp;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Converts only whole points; both simulation and execution belong to one transaction. */
public final class XpFluidTransfer {
    public static int insert(ResourceHandler<FluidResource> tank,FluidResource fluid,int points,int unitsPerPoint) {
        int requested=Math.min(points,Integer.MAX_VALUE/unitsPerPoint)*unitsPerPoint;
        if (requested<=0) return 0;
        try (var transaction=Transaction.openRoot()) {
            int accepted;
            try (var simulation=Transaction.open(transaction)) {
                accepted=tank.insert(fluid,requested,simulation)/unitsPerPoint*unitsPerPoint;
            }
            if (accepted==0) return 0;
            int inserted=tank.insert(fluid,accepted,transaction);
            if (inserted%unitsPerPoint!=0) return 0;
            transaction.commit();
            return inserted/unitsPerPoint;
        }
    }
    private XpFluidTransfer() {}
}
