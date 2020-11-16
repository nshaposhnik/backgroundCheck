import { Platform, NativeModules } from 'react-native';

export const beginBackgroundTask = async () => {
  if (Platform.OS === 'ios') {
    return await NativeModules.beginBackgroundTask.begin()
  }
}

export const endBackgroundTask = async (backgroundTaskId) => {
  if (Platform.OS === 'ios') {
    await NativeModules.beginBackgroundTask.end(backgroundTaskId)
  }
}

export const backgroundTimeRemaining = async () => {
  if (Platform.OS === 'ios') {
    return await NativeModules.beginBackgroundTask.remaining()
  }
}
