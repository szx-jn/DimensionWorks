import React from "react";
import { Composition } from "remotion";
import { Intro } from "./Intro";
export const Root: React.FC = () => <Composition id="DimensionWorksIntro" component={Intro} durationInFrames={2700} fps={30} width={1920} height={1080} />;
