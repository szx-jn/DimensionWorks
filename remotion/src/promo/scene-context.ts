import {createContext, useContext} from 'react';
export const ShotContext = createContext({id: 0, frames: 120});
export const useShot = () => useContext(ShotContext);
