# What is SDRTrunk Kennebec?

SDRTrunk Kennebec is a computer program that acts like a highly advanced police scanner. It uses a small, inexpensive USB antenna (called an SDR) to listen to local radio conversations, like police, fire, or EMS.

> **Note:**
> SDRTrunk Kennebec is currently in **prerelease**. This means it is still getting final polishes, but it works great for everyday listening!

## How it works: The "Highway" Analogy

In the past, police and fire departments used dedicated radio channels. Think of this like driving on a private one-lane road. It was easy to listen to, but they ran out of roads quickly!

Modern radio systems use **Trunking**. Trunking is like a massive multi-lane highway with a traffic cop directing cars:
* When an officer wants to talk, the "traffic cop" (the radio system) points them to an empty lane (frequency) just for that sentence.
* When they finish talking, that lane is freed up for someone else.

Traditional scanners get confused because the conversations are constantly jumping between lanes. **SDRTrunk Kennebec solves this.** It acts like a helicopter floating above the entire highway, watching every lane at the same time and perfectly grouping the conversations together for you to hear.

## Supported Radio Types

SDRTrunk Kennebec understands many different radio "languages":

| Radio Type | Simple Description |
|---|---|
| **Project 25 (P25)** | The most common modern digital radio used by police and fire in North America. |
| **DMR** | Commonly used by businesses, casinos, and some local government. |
| **Analog FM** | The classic, old-school radio style. |

> **Tip:**
> If you don't know what your local area uses, don't worry! You can look up your county on [RadioReference.com](https://www.radioreference.com) to find out.

## Why the "Kennebec" Version?

The Kennebec version takes the original SDRTrunk app and makes it much easier to use, especially if you want to leave it running 24/7 or send your audio to your phone.

* **Beautiful Interface:**A modern look with dark mode and simple menus.
* **AI Features:**Smart features that automatically clean up bad audio or summarize calls.
* **Easy Streaming:**Send your audio directly to apps like Zello or Broadcastify.
* **Self-Healing:**If your USB antenna crashes, Kennebec can automatically restart it without you needing to do anything.

## Where to go next?
Ready to dive in? Check out our [Quickstart Guide](quickstart.md)!
