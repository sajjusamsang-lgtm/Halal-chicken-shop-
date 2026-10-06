const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- SECURITY BOUNDS TESTS ---

test("Unauthenticated user: cannot read shop", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("shops").doc(ALICE_UID).get());
});

test("Unauthenticated user: cannot read customer subcollection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("shops").doc(ALICE_UID).collection("customers").get());
});

test("Authenticated owner: can create and read their own shop", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("shops").doc(ALICE_UID).set({
      shopId: ALICE_UID,
      ownerId: ALICE_UID,
      shopName: "Halal Chicken Shop Hanti",
      ownerName: "Mr. Sajid",
      phone: "+91 9876543210"
    })
  );
  await assertSucceeds(aliceDb.collection("shops").doc(ALICE_UID).get());
});

test("Authenticated user: cannot read another owner's shop", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("shops").doc(BOB_UID).set({
      shopId: BOB_UID,
      ownerId: BOB_UID,
      shopName: "Bob's Poultry",
      ownerName: "Bob"
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("shops").doc(BOB_UID).get());
});

test("Authenticated owner: can create and read customer under own shop", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("shops").doc(ALICE_UID).collection("customers").doc("cust_1").set({
      id: "cust_1",
      shopId: ALICE_UID,
      name: "Delhi Darbar Restaurant",
      phone: "+91 9876543210",
      customerType: "RESTAURANT",
      totalPending: 0
    })
  );
  await assertSucceeds(
    aliceDb.collection("shops").doc(ALICE_UID).collection("customers").doc("cust_1").get()
  );
});

test("Authenticated user: cannot access another owner's customers", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("shops").doc(BOB_UID).collection("customers").doc("cust_bob").set({
      id: "cust_bob",
      shopId: BOB_UID,
      name: "Secret Hotel",
      phone: "123",
      customerType: "HOTEL"
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("shops").doc(BOB_UID).collection("customers").doc("cust_bob").get()
  );
});

test("Authenticated owner: can create sale and live chicken purchase", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("shops").doc(ALICE_UID).collection("chickenPurchases").doc("purch_1").set({
      id: "purch_1",
      shopId: ALICE_UID,
      supplierName: "Raza Poultry Farm",
      numberOfBirds: 150,
      totalLiveWeightKg: 340.5,
      purchaseRatePerKg: 135.0,
      totalCost: 45967.5
    })
  );

  await assertSucceeds(
    aliceDb.collection("shops").doc(ALICE_UID).collection("sales").doc("sale_1").set({
      id: "sale_1",
      shopId: ALICE_UID,
      billNumber: "INV-20261004-001",
      customerName: "Green Valley Public School",
      productType: "CHICKEN",
      finalAmount: 4800.0,
      paymentStatus: "PARTIAL",
      paymentMethod: "UPI"
    })
  );
});
