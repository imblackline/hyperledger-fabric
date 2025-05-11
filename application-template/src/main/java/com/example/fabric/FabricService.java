package com.example.fabric;

import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.CommitException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;

@Service
public class FabricService {
    private static final Path PATH_TO_TEST_NETWORK = Paths.get("/home/imblackline/go/src/github.com/imblackline/fabric-samples/test-network");
    private static final String CHANNEL_NAME = System.getenv().getOrDefault("CHANNEL_NAME", "mychannel");
    private static final String CHAINCODE_NAME = System.getenv().getOrDefault("CHAINCODE_NAME", "basic");
    private static final String PEER_ENDPOINT = "localhost:7051";
    private static final String OVERRIDE_AUTH = "peer0.org1.example.com";

    private Gateway gateway;
    private Contract contract;
    private ManagedChannel channel;

    public FabricService() throws Exception {
        initializeConnection();
    }

    private void initializeConnection() throws Exception {
        ChannelCredentials credentials = TlsChannelCredentials.newBuilder()
                .trustManager(PATH_TO_TEST_NETWORK.resolve(Paths.get(
                        "organizations/peerOrganizations/org1.example.com/" +
                                "peers/peer0.org1.example.com/tls/ca.crt"))
                        .toFile())
                .build();

        channel = Grpc.newChannelBuilder(PEER_ENDPOINT, credentials)
                .overrideAuthority(OVERRIDE_AUTH)
                .build();

        Gateway.Builder builder = Gateway.newInstance()
                .identity(new X509Identity("Org1MSP",
                        Identities.readX509Certificate(
                                Files.newBufferedReader(
                                        PATH_TO_TEST_NETWORK.resolve(Paths.get(
                                                "organizations/peerOrganizations/org1.example.com/" +
                                                        "users/User1@org1.example.com/msp/signcerts/cert.pem"))))))
                .signer(
                        Signers.newPrivateKeySigner(
                                Identities.readPrivateKey(
                                        Files.newBufferedReader(
                                                Files.list(PATH_TO_TEST_NETWORK.resolve(
                                                        Paths.get(
                                                                "organizations/peerOrganizations/org1.example.com/"
                                                                        +
                                                                        "users/User1@org1.example.com/msp/keystore")))
                                                        .findFirst().orElseThrow()))))
                .connection(channel)
                .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
                .submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));

        gateway = builder.connect();
        contract = gateway.getNetwork(CHANNEL_NAME).getContract(CHAINCODE_NAME);
    }

    public String createBasil(String id, String country) throws GatewayException, CommitException {
        byte[] result = contract.submitTransaction("createBasil", id, country);
        return new String(result, StandardCharsets.UTF_8);
    }

    public String readBasil(String id) throws GatewayException {
        byte[] result = contract.evaluateTransaction("readBasil", id);
        return new String(result, StandardCharsets.UTF_8);
    }

    public void cleanup() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }
} 