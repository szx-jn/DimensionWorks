import React from "react";
import {Composition, Folder} from "remotion";
import {Intro} from "./Intro";
import {Promo} from "./promo/Promo";
import {PromoScene} from "./promo/PromoScene";
export const Root:React.FC=()=> <>
  <Composition id="DimensionWorksIntro" component={Intro} durationInFrames={2700} fps={30} width={1920} height={1080}/>
  <Composition id="DimensionWorksPromo" component={Promo} durationInFrames={10800} fps={30} width={1920} height={1080}/>
  <Folder name="PromoScenes">
    <Composition id="PromoScene00" component={PromoScene} defaultProps={{shotId:0}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene01" component={PromoScene} defaultProps={{shotId:1}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene02" component={PromoScene} defaultProps={{shotId:2}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene03" component={PromoScene} defaultProps={{shotId:3}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene04" component={PromoScene} defaultProps={{shotId:4}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene05" component={PromoScene} defaultProps={{shotId:5}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene06" component={PromoScene} defaultProps={{shotId:6}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene07" component={PromoScene} defaultProps={{shotId:7}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene08" component={PromoScene} defaultProps={{shotId:8}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene09" component={PromoScene} defaultProps={{shotId:9}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene10" component={PromoScene} defaultProps={{shotId:10}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene11" component={PromoScene} defaultProps={{shotId:11}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene12" component={PromoScene} defaultProps={{shotId:12}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene13" component={PromoScene} defaultProps={{shotId:13}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene14" component={PromoScene} defaultProps={{shotId:14}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene15" component={PromoScene} defaultProps={{shotId:15}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene16" component={PromoScene} defaultProps={{shotId:16}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene17" component={PromoScene} defaultProps={{shotId:17}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene18" component={PromoScene} defaultProps={{shotId:18}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene19" component={PromoScene} defaultProps={{shotId:19}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene20" component={PromoScene} defaultProps={{shotId:20}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene21" component={PromoScene} defaultProps={{shotId:21}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene22" component={PromoScene} defaultProps={{shotId:22}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene23" component={PromoScene} defaultProps={{shotId:23}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene24" component={PromoScene} defaultProps={{shotId:24}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene25" component={PromoScene} defaultProps={{shotId:25}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene26" component={PromoScene} defaultProps={{shotId:26}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene27" component={PromoScene} defaultProps={{shotId:27}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene28" component={PromoScene} defaultProps={{shotId:28}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene29" component={PromoScene} defaultProps={{shotId:29}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene30" component={PromoScene} defaultProps={{shotId:30}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene31" component={PromoScene} defaultProps={{shotId:31}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene32" component={PromoScene} defaultProps={{shotId:32}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene33" component={PromoScene} defaultProps={{shotId:33}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene34" component={PromoScene} defaultProps={{shotId:34}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene35" component={PromoScene} defaultProps={{shotId:35}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene36" component={PromoScene} defaultProps={{shotId:36}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene37" component={PromoScene} defaultProps={{shotId:37}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene38" component={PromoScene} defaultProps={{shotId:38}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene39" component={PromoScene} defaultProps={{shotId:39}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene40" component={PromoScene} defaultProps={{shotId:40}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene41" component={PromoScene} defaultProps={{shotId:41}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene42" component={PromoScene} defaultProps={{shotId:42}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene43" component={PromoScene} defaultProps={{shotId:43}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene44" component={PromoScene} defaultProps={{shotId:44}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene45" component={PromoScene} defaultProps={{shotId:45}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene46" component={PromoScene} defaultProps={{shotId:46}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene47" component={PromoScene} defaultProps={{shotId:47}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene48" component={PromoScene} defaultProps={{shotId:48}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene49" component={PromoScene} defaultProps={{shotId:49}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene50" component={PromoScene} defaultProps={{shotId:50}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene51" component={PromoScene} defaultProps={{shotId:51}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene52" component={PromoScene} defaultProps={{shotId:52}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene53" component={PromoScene} defaultProps={{shotId:53}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene54" component={PromoScene} defaultProps={{shotId:54}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene55" component={PromoScene} defaultProps={{shotId:55}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene56" component={PromoScene} defaultProps={{shotId:56}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene57" component={PromoScene} defaultProps={{shotId:57}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene58" component={PromoScene} defaultProps={{shotId:58}} durationInFrames={180} fps={30} width={1920} height={1080}/>
    <Composition id="PromoScene59" component={PromoScene} defaultProps={{shotId:59}} durationInFrames={180} fps={30} width={1920} height={1080}/>
  </Folder>
</>;
