import {Composition} from 'remotion';
import {DimensionWorksOpening} from './DimensionWorksOpening';

export const RemotionRoot: React.FC = () => {
  return (
    <Composition
      id="DimensionWorksOpening"
      component={DimensionWorksOpening}
      durationInFrames={1030}
      fps={30}
      width={1920}
      height={1080}
    />
  );
};
