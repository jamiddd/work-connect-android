import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import algoliasearch from "algoliasearch";
import { User } from "./User";
// import { User } from "./User";

admin.initializeApp();

const ALGOLIA_ID = "4DNBNXMOP5";
const ALGOLIA_ADMIN_KEY = "3b7e8649b7966af749e7cc95332bef01";
const ALGOLIA_INDEX_NAME = "users";
const client = algoliasearch(ALGOLIA_ID, ALGOLIA_ADMIN_KEY);
// // const ALGOLIA_SEARCH_KEY = functions.config().algolia.search_key;

// const ALGOLIA_INDEX_NAME = "users";
// const client = algoliasearch(ALGOLIA_ID, ALGOLIA_ADMIN_KEY)

// export const onUserCreated = functions.firestore.document("posts/{postId}").onCreate(async (snapshot, context) => {
//     const post = snapshot.data();
//     console.log("Post - " + post);
//     console.log("Context - " + context)
// });

export const addIndexToAlgolia = functions.https.onCall((data, context) => {
    const user: User = {
        uid: data.uid,
        name: data.name,
        photo: data.photo,
        username: data.username,
        email: data.email,
        objectID: data.uid
    };

    console.log("User - " + user);

    const index = client.initIndex(ALGOLIA_INDEX_NAME);
    return index.saveObject(user);
});