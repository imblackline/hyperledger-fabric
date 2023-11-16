/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;

public final class App {

	// path to your test-network directory included, e.g.: Paths.get("..", "..", "test-network")
	private static final Path PATH_TO_TEST_NETWORK = Paths.get("..", "..", "test-network");

	private static final String CHANNEL_NAME = System.getenv().getOrDefault("CHANNEL_NAME", "mychannel");
	private static final String CHAINCODE_NAME = System.getenv().getOrDefault("CHAINCODE_NAME", "basic");

	// Gateway peer end point.
	private static final String PEER_ENDPOINT = "localhost:7051";
	private static final String OVERRIDE_AUTH = "peer0.org1.example.com";

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public static void main(final String[] args) throws Exception {

		ChannelCredentials credentials = TlsChannelCredentials.newBuilder()
				.trustManager(PATH_TO_TEST_NETWORK.resolve(Paths.get(
						"organizations/peerOrganizations/org1.example.com/" +
								"peers/peer0.org1.example.com/tls/ca.crt"))
						.toFile())
				.build();
		// The gRPC client connection should be shared by all Gateway connections to
		// this endpoint.
		ManagedChannel channel = Grpc.newChannelBuilder(PEER_ENDPOINT, credentials)
				.overrideAuthority(OVERRIDE_AUTH)
				.build();
		
		Gateway.Builder builderOrg1 = Gateway.newInstance()
				.identity(new X509Identity("Org1MSP",
						Identities.readX509Certificate(
							Files.newBufferedReader(
								PATH_TO_TEST_NETWORK.resolve(Paths.get(
									"organizations/peerOrganizations/org1.example.com/" +
									"users/User1@org1.example.com/msp/signcerts/cert.pem"
								))
							)
						)
					))
				.signer(
					Signers.newPrivateKeySigner(
						Identities.readPrivateKey(
							Files.newBufferedReader(
								Files.list(PATH_TO_TEST_NETWORK.resolve(
									Paths.get(
										"organizations/peerOrganizations/org1.example.com/" +
										"users/User1@org1.example.com/msp/keystore")
									)
								).findFirst().orElseThrow()
							)
						)
					)
				)
				.connection(channel)
				// Default timeouts for different gRPC calls
				.evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
				.endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
				.submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
				.commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));

		// notice that we can share the grpc connection since we don't use private date,
		// otherwise we should create another connection
		Gateway.Builder builderOrg2 = Gateway.newInstance()
				.identity(new X509Identity("Org2MSP",
						Identities.readX509Certificate(Files.newBufferedReader(PATH_TO_TEST_NETWORK.resolve(Paths.get(
								"organizations/peerOrganizations/org2.example.com/users/User1@org2.example.com/msp/signcerts/cert.pem"))))))
				.signer(Signers.newPrivateKeySigner(Identities.readPrivateKey(Files.newBufferedReader(Files
						.list(PATH_TO_TEST_NETWORK.resolve(Paths
								.get("organizations/peerOrganizations/org2.example.com/users/User1@org2.example.com/msp/keystore")))
						.findFirst().orElseThrow()))))
				.connection(channel)
				.evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
				.endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
				.submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
				.commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));
		
		try (Gateway gatewayOrg1 = builderOrg1.connect();
				Gateway gatewayOrg2 = builderOrg2.connect()) {
			
			Contract contractOrg1 = gatewayOrg1
				.getNetwork(CHANNEL_NAME)
				.getContract(CHAINCODE_NAME);
			
			Contract contractOrg2 = gatewayOrg2
				.getNetwork(CHANNEL_NAME)
				.getContract(CHAINCODE_NAME);
			
			byte[] result;
			result = contractOrg1.submitTransaction("CreateAsset",
				"assetId1", "yellow", "5", "Tom", "1300");
			System.out.println("Create result= " + new String(result));

			result = contractOrg1.evaluateTransaction("ReadAsset", 
				"assetId1");
			System.out.println("Query result= " + new String(result));
			
		} finally {
			channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
		}
	}

	/**
	 * Evaluate a transaction to query ledger state.
	 */
	private void getAllAssets(Contract contract) throws GatewayException {
		System.out.println(
				"\n--> Evaluate Transaction: GetAllAssets, function returns all the current assets on the ledger");

		var result = contract.evaluateTransaction("GetAllAssets");

		System.out.println("*** Result: " + prettyJson(result));
	}

	private String prettyJson(final byte[] json) {
		return prettyJson(new String(json, StandardCharsets.UTF_8));
	}

	private String prettyJson(final String json) {
		var parsedJson = JsonParser.parseString(json);
		return gson.toJson(parsedJson);
	}
}
