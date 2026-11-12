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

test("Unauthenticated user cannot read or write adventurer profiles, quests, badges, inventory, or artifacts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("quests").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("badges").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("inventory").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("artifacts").get());
});

test("Authenticated user can create and update their own AdventurerProfile, but cannot spoof another user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Vesper the Scout",
      characterStyle: "CYBERPUNK_SCOUT",
      xp: 150,
      level: 2,
      questsCompleted: 1,
      landmarksVisited: 3,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject shadow field injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      xp: 300,
      isAdmin: true,
      updatedAt: new Date(),
    })
  );

  // Bob cannot read Alice's profile
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user can create, list, and complete quests, and terminal COMPLETED quests lock further updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const questRef = aliceDb.collection("users").doc(ALICE_UID).collection("quests").doc("quest_1");

  await assertSucceeds(
    questRef.set({
      userId: ALICE_UID,
      title: "Neon Shadows of Pioneer Park",
      characterStyle: "CYBERPUNK_SCOUT",
      regionName: "Downtown Sector",
      summary: "Scanning old-world monuments for encrypted signal relays.",
      landmarksCount: 3,
      completedStops: 1,
      xpEarned: 100,
      distanceMeters: 850,
      status: "ACTIVE",
      stopsSummary: ["Pioneer Plaque", "Central Park Fountain", "Blue Bottle Cafe"],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("quests").get()
  );

  // Complete the quest
  await assertSucceeds(
    questRef.update({
      completedStops: 3,
      xpEarned: 350,
      status: "COMPLETED",
      summary: "All signal relays decrypted.",
      updatedAt: new Date(),
    })
  );

  // Terminal state lock: cannot update once status is COMPLETED
  await assertFails(
    questRef.update({
      xpEarned: 9999,
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user can unlock badges, manage inventory items, and catalog found artifacts with strict isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  const badgeRef = aliceDb.collection("users").doc(ALICE_UID).collection("badges").doc("badge_first_step");
  await assertSucceeds(
    badgeRef.set({
      userId: ALICE_UID,
      badgeCode: "FIRST_STEP",
      title: "Chronicle Initiate",
      description: "Completed your first real-world landmark scan.",
      category: "EXPLORATION",
      rarity: "COMMON",
      landmarkName: "Heritage Oak Park",
      createdAt: now,
    })
  );

  // Badges are immutable after creation
  await assertFails(
    badgeRef.update({
      rarity: "LEGENDARY",
    })
  );

  const itemRef = aliceDb.collection("users").doc(ALICE_UID).collection("inventory").doc("item_compass_1");
  await assertSucceeds(
    itemRef.set({
      userId: ALICE_UID,
      name: "Aetherial Brass Astrolabe",
      loreDescription: "Recovered near the Victorian Sundial monument.",
      itemType: "RELIC",
      rarity: "RARE",
      originLandmark: "Victorian Sundial",
      characterStyle: "VICTORIAN_HISTORIAN",
      powerBonus: 25,
      equipped: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Can toggle equipped state
  await assertSucceeds(
    itemRef.update({
      equipped: true,
      updatedAt: new Date(),
    })
  );

  // Found Artifact creation & inspection note updates
  const artifactRef = aliceDb.collection("users").doc(ALICE_UID).collection("artifacts").doc("artifact_plaque_1");
  await assertSucceeds(
    artifactRef.set({
      userId: ALICE_UID,
      artifactCode: "ART_PONY_EXPRESS_1860",
      name: "Pony Express Brass Seal Rubbing",
      loreInscription: "Imprinted from California Historical Landmark #696 in San Francisco.",
      category: "PLAQUE_RUBBING",
      rarity: "EPIC",
      landmarkName: "Pony Express Western Terminus Plaque",
      regionName: "San Francisco — Embarcadero",
      characterStyle: "VICTORIAN_HISTORIAN",
      latitude: 37.7945,
      longitude: -122.4018,
      resonanceValue: 180,
      isFavorite: false,
      inspectionNotes: "Spotted near the corner arcade.",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Owner can toggle favorite & update inspection notes
  await assertSucceeds(
    artifactRef.update({
      isFavorite: true,
      inspectionNotes: "Pinned to my top showcase curios!",
      updatedAt: new Date(),
    })
  );

  // Owner cannot mutate rarity or resonanceValue after creation
  await assertFails(
    artifactRef.update({
      resonanceValue: 999,
      updatedAt: new Date(),
    })
  );

  // Bob cannot list Alice's inventory, badges, or artifacts
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("inventory").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("badges").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("artifacts").get());
});
