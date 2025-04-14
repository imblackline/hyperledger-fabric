package org.hyperledger.fabric.samples;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;

import com.owlike.genson.Genson;

import java.util.ArrayList;
import java.util.List;

@Contract(
        name = "BasilContract",
        info = @Info(
                title = "My Smart contract",
                description = "The hyperlegendary asset transfer",
                version = "1.0",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "a.transfer@example.com",
                        name = "Adrian Transfer",
                        url = "https://hyperledger.example.com")))
@Default
public class BasilContract implements ContractInterface {

    private final Genson genson = new Genson();

    // Create a new basil plant
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void createBasil(Context ctx, String qrCode, String origin) {
        ChaincodeStub stub = ctx.getStub();

        if (stub.getStringState(qrCode) != null && !stub.getStringState(qrCode).isEmpty()) {
            throw new ChaincodeException("Basil already exists with QR: " + qrCode);
        }
 
        String orgId = ctx.getClientIdentity().getMSPID();
        Owner owner = new Owner(orgId, "Greenhouse");
        Long creationTimestamp = stub.getTxTimestamp().getEpochSecond();

        Basil basil = new Basil(qrCode, creationTimestamp, origin, "Created", null, owner, new ArrayList<>());

        stub.putStringState(qrCode, genson.serialize(basil));
    }

    // Stop tracking a basil plant (delete it)
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deleteBasil(Context ctx, String qrCode) {
        ChaincodeStub stub = ctx.getStub();
        Basil basil = readBasil(ctx, qrCode);

        String clientOrg = getClientOrgId(ctx);
        if (!basil.getCurrentOwner().getOrgId().equals(clientOrg)) {
            throw new ChaincodeException("Only the current owner can delete the plant.");
        }

        stub.delState(qrCode);
    }

    // Update state: add a BasilLeg and change current info
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void updateBasilState(Context ctx, String qrCode, String gps, Long timestamp, String temp, String humidity,
            String status) {
        ChaincodeStub stub = ctx.getStub();
        Basil basil = readBasil(ctx, qrCode);

        String clientOrg = getClientOrgId(ctx);
        if (!basil.getCurrentOwner().getOrgId().equals(clientOrg)) {
            throw new ChaincodeException("Only the current owner can update the plant state.");
        }

        Owner owner = basil.getCurrentOwner();
        BasilLeg leg = new BasilLeg(timestamp, gps, temp, humidity, owner);
        basil.getTransportHistory().add(leg);

        Basil updated = new Basil(
                basil.getQrCode(),
                basil.getCreationTimestamp(),
                basil.getOrigin(),
                status,
                gps,
                owner,
                basil.getTransportHistory());

        stub.putStringState(qrCode, genson.serialize(updated));
    }

    // Get current state of a basil plant
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Basil readBasil(Context ctx, String qrCode) {
        String data = ctx.getStub().getStringState(qrCode);
        if (data == null || data.isEmpty()) {
            throw new ChaincodeException("No basil found with QR: " + qrCode);
        }
        return genson.deserialize(data, Basil.class);
    }

    // Get the transport history
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public List<BasilLeg> getHistory(Context ctx, String qrCode) {
        Basil basil = readBasil(ctx, qrCode);
        return basil.getTransportHistory();
    }

    // Transfer ownership to another organization
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void transferOwnership(Context ctx, String qrCode, String newOrgId, String newName) {
        ChaincodeStub stub = ctx.getStub();
        Basil basil = readBasil(ctx, qrCode);

        String clientOrg = getClientOrgId(ctx);
        if (!basil.getCurrentOwner().getOrgId().equals(clientOrg)) {
            throw new ChaincodeException("Only the current owner can transfer ownership.");
        }

        Owner newOwner = new Owner(newOrgId, newName);
        Basil updated = new Basil(
                basil.getQrCode(),
                basil.getCreationTimestamp(),
                basil.getOrigin(),
                basil.getCurrentStatus(),
                basil.getCurrentGps(),
                newOwner,
                basil.getTransportHistory());

        stub.putStringState(qrCode, genson.serialize(updated));
    }

    private String getClientOrgId(Context ctx) {
        // In a real app, use the client's certificate to derive the org
        return ctx.getClientIdentity().getMSPID();
    }
}
