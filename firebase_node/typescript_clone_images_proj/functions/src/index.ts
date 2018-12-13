import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
admin.initializeApp();

import * as Storage from '@google-cloud/storage';
const gcs = new Storage();

import { tmpdir } from 'os';
import { join, dirname } from 'path';

import * as sharp from 'sharp';
import * as fs from 'fs-extra';

export const sendNotification = functions.database.ref('chats/{id}')
    .onWrite((change, context) => {
        const beforeData = change.before.val();
        const actualData = change.after.val();
        if(context.auth === undefined || context.auth === null){
            console.log("User isn't authenticated?!");
            console.log(context);
            return false;
        }
        const senderId = context.auth.uid;
        const senderName = (context.auth.token as any).name;
        if(beforeData.users === undefined || beforeData.users === null){
            console.log("User " + senderName + " is using a old version!");
            return false;
        }
        if(actualData.users === null || beforeData.users === null){
            console.log("Ignore notification as it isn't necessary ");
            return false;
        }
        const usersActual = actualData.users.toString().split('&');
        let message2Send = null;
        if(beforeData.messages === undefined || beforeData.messages === null){
            message2Send = "You were added to this chat!";
        }
        if(message2Send === null && actualData.timeStamp.toString() === beforeData.timeStamp.toString()){
            console.log("Ignore notification as it isn't necessary ");
            return false;
        }
        const getUserPromisses = [];
        for(let i = 0; i < usersActual.length; i++){
            if(usersActual[i] === senderId) continue;
            getUserPromisses.push(admin.database()
                .ref('Notifications/' + usersActual[i]).child("token").once('value'));

        }
        const tokens = [];
        if(getUserPromisses.length === 0) return false;
        return Promise.all(getUserPromisses).then((results:any[]) => {
            for(let i = 0; i < results.length; i++){
                if(results[i].val().toString() === 'offline') continue;
                tokens.push(results[i].val());
            }

            if(message2Send === null) message2Send = senderName + ': ' + actualData.lastMessage.toString();

            const payload = {
                notification: {
                    title: 'New message in ' + actualData.chatName.toString(),
                    body: message2Send
                }
            };
            if(tokens.length === 0)return true;
            // Send notifications to all tokens.
            return admin.messaging().sendToDevice(tokens, payload).then((response:any)=>{
                response.results.forEach((result, index) => {
                    const error = result.error;
                    if (error) {
                        console.error('Failure sending notification to', tokens[index], error);
                    }
                })
                return true;
            });
        });
    });

export const resizeImages = functions.storage
    .object()
    .onFinalize(async object => {
        const bucket = gcs.bucket(object.bucket);
        const filePath = object.name;
        const fileName = filePath.split('/').pop();
        const bucketDir = dirname(filePath);
        const contentType = object.contentType;
        const metadata = object.metadata;
        const originalSize = +metadata.height + (+metadata.width);
        const workingDir = join(tmpdir(), 'resize');
        const string = 'omg1' + fileName.toString().split(':').pop() + '.png';
        const tmpFilePath = join(workingDir, string);

        if (fileName.includes('low@') || fileName.includes('high@') || !object.contentType.includes('image')) {
            console.log('exiting function');
            return false;
        }

        // 1. Ensure thumbnail dir exists
        await fs.ensureDir(workingDir);

        // 2. Download Source File
        await bucket.file(filePath).download({
            destination: tmpFilePath
        });

        // 3. Resize the images and define an array of upload promises
        const sizes = [];

        //if the metadata is null original size will be 0 so you should still convert it anyway
        if(originalSize === 0 || originalSize > 1280+960){
            sizes.push({name:'high',width: 1280,height: 960})
        }

        if(originalSize === 0 || originalSize > 640+480){
            sizes.push({name: 'low',width: 640,height: 480});
        }

        const uploadPromises = sizes.map(async size => {
            const thumbName = `${size.name}@_${fileName}`;
            const thumbPath = join(workingDir, thumbName);

            // Resize source image
            await sharp(tmpFilePath)
                .resize(size.width, size.height)
                .ignoreAspectRatio()
                .toFile(thumbPath);

            metadata.width = "" + size.width;
            metadata.height = "" + size.height;

            // Upload to GCS
            return bucket.upload(thumbPath, {
                destination: join(bucketDir, thumbName),
                contentType: contentType,
                metadata: {
                    contentType: contentType,
                    metadata: metadata,
                }
            });
        });

        // 4. Run the upload operations
        await Promise.all(uploadPromises);

        //force delete
        await fs.remove(workingDir);

        // 5. Cleanup remove the tmp/thumbs from the filesystem
        return fs.remove(workingDir);
    });