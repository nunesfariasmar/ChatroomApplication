# Real-time multimedia chat
Mobile Cloud Computing Project for the Fall Semester of 2018, Aalto University. This project was developed by João Loureiro, Bruno Barros, Mariana Farias, Antonio Longa and Lakshay Bandlish of Group 18.

### How to run
In order to deploy, just need to run the deploy script:
```console
$ sh deploy.sh
```
This will generate and install the apk and also deploy the cloud functions
In order to just build and install the apk run the install script:
```console
$ sh install.sh
```

### Project Structure
__editProfile Directory__ All Java classes used for editing the profile are present in this package.
+ __EditProfile:__ Loads user profile and provides the option to change the display name, password and picture.
+ __PopActivity:__  Used to change password.
+ __PopPicture:__ Used to change picture.

__Notifications__ Classes used in handling Message Services and getting the FCM token.
+ __MessageService:__ Handle Notifications.
+ __MyFirebaseService:__ Handle FCM tokens.

__Objects__ Object classes used for storing information needed to inflate views.
+ __ChatItem:__ Saves information relative to the chat to be displayed in the user's chat list.
+ __FriendlyMessage:__ Saves information relative to one message in a chat.
+ __NotificationItem:__ Saves information relative to a notification.
+ __UserItem:__ Saves information relative to a user.

__Utils__ Classes for the Firebase functions.
+ __Labeler:__ Labels pictures.
+ __MLOptions:__ Saves labels to be displayed in the gallery when listing picture by cathegory.
+ __PicResolution:__ Saves information relative to picture's resolutions.
+ __Uploader:__ Class responsible for uploading user generated files (images) to the cloud server.

__Remaining Classes__
+ __AddUser:__ Class responsible for adding new users to new or existing chatrooms.
+ __ChatInfoActivity:__ Class responsible for listing chatroom's information such as users in the chatroom and chatroom name.
+ __HomeLogIn:__ Class for displaying the LogIn interface. Gives user the option to log in or sign up. This is the displayed activity when we run the app.
+ __GalleryActivity:__ Used to display the gallery in a chat.
+ __ImageDisplay:__ Class for displaying images after click, either on the chatroom or on the gallery. It gives the user the option to obtain the full resolution image.
+ __MainActivity:__ Class for displaying all chatrooms a user belongs too. It gives the user the possibility to select a chat room, create a new chatroom, change his/her settings and log out.
+ __Settings:__ Class for storing users settings relating to picture's resolutions.
+ __SigIn:__ Class for user sign in.
+ __SingleChatActivity:__ Class for displaying all messages in a chatroom. It gives users the possibility to send messages, images either via camera or gallery (through the + button), access the chat information and gallery. It also let's users add new users or leave a chatroom.

### Firebase Node
Inside the Firebase Node directory we can find one folder which contains the Firebase Cloud functions Project to deploy to the server.

There are 2 functions available for this project, being both of them written in Typescript and are available on the index.js file located at 
```
firebase_node/typescript_clone_images_proj/functions/src
```
The following is a brief description of both of these functions:
+ __sendNotification:__ is a function responsible for sending notifications to every member of the chat, with the exception of the sender of the message. This is triggered everytime there is an update of the message group on the realtime database and it sends a notification whenever it is necessary (new messages sent, additions and removal of users).
+ __resizeImages:__ is a function responsible for cloning the images stored on the Firebase Storage but with a lower resolution. This will verify if the just uploaded image by the user has an adequate size (width+height) and will create a resize version for the high and low resolutions if those are smaller than the original one.

The libraries/frameworks used were the __firebase-functions__ and __firebase-admin__, as well as __sharp__ for the image resizing and __fs-extra__ for folder and file manipulation.