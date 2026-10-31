import React from "react";

/** Original block illustration of Steve; no third-party skin or model asset. */
export const Steve: React.FC<{x: number; y: number; scale?: number; tilt?: number; time: number}> = ({x, y, scale = 2.6, tilt = 0, time}) => {
  const sway = Math.sin(time * 1.6) * 1.6;
  return <g transform={`translate(${x} ${y}) scale(${scale})`} shapeRendering="crispEdges">
    <ellipse cx="0" cy="132" rx="39" ry="5" fill="#102629" opacity=".35"/>
    <g transform={`rotate(${tilt} 0 125)`}>
      <g transform="translate(-16 0)">
        <path d="M0 0 L8 -6 H40 L32 0Z" fill="#523a29"/>
        <path d="M32 0 L40 -6 V26 L32 32Z" fill="#855a42"/>
        <rect width="32" height="32" fill="#bc8965"/>
        <path d="M0 0 H32 V9 H28 V5 H20 V8 H8 V12 H0Z" fill="#34271f"/>
        <rect x="4" y="14" width="8" height="4" fill="#e4e0d3"/><rect x="20" y="14" width="8" height="4" fill="#e4e0d3"/>
        <rect x="8" y="14" width="4" height="4" fill="#646caa"/><rect x="20" y="14" width="4" height="4" fill="#646caa"/>
        <rect x="12" y="18" width="8" height="5" fill="#9e694e"/>
        <path d="M8 23 H12 V27 H20 V23 H24 V31 H8Z" fill="#50352a"/>
        <rect x="12" y="23" width="8" height="4" fill="#784a38"/>
        <path d="M0 18 H4 V27 H0 M28 19 H32 V27 H28" fill="#ad7957"/>
      </g>
      <path d="M-16 32 H16 L24 26 V74 L16 80 H-16Z" fill="#177d82"/>
      <rect x="-16" y="32" width="32" height="48" fill="#249eaa"/>
      <path d="M-8 32 H8 V38 H4 V42 H-4 V38 H-8Z" fill="#ba8866"/>
      <path d="M-12 45 H-8 V70 H-12 M7 50 H11 V76 H7" fill="none" stroke="#208f9a" strokeWidth="4"/>
      <path d="M-16 78 H16 V82 H-16Z" fill="#267b88"/>
      <g transform={`rotate(${sway} -18 34)`}>
        <path d="M-32 34 H-16 V79 H-32Z" fill="#bd8863"/><rect x="-32" y="34" width="16" height="17" fill="#2499a5"/>
        <path d="M-32 60 H-28 V75 H-32 M-20 52 H-16 V79 H-20" fill="#a97554"/>
      </g>
      <g transform={`rotate(${-sway} 18 34)`}>
        <path d="M16 34 L24 28 H40 V74 L32 80 H16Z" fill="#8f654c"/>
        <rect x="16" y="34" width="16" height="46" fill="#bc8965"/><rect x="16" y="34" width="16" height="17" fill="#2499a5"/>
        <path d="M32 34 L40 28 V45 L32 51Z" fill="#187782"/>
        <rect x="16" y="67" width="4" height="9" fill="#ce9a72"/>
      </g>
      <rect x="-16" y="80" width="15" height="48" fill="#5056a0"/><rect x="1" y="80" width="15" height="48" fill="#454b91"/>
      <path d="M16 80 L24 74 V122 L16 128Z" fill="#363c77"/>
      <rect x="-13" y="87" width="4" height="25" fill="#5b62aa"/><rect x="4" y="94" width="4" height="18" fill="#50589d"/>
      <rect x="-16" y="121" width="15" height="7" fill="#3b4655"/><rect x="1" y="121" width="15" height="7" fill="#35404f"/>
    </g>
  </g>;
};
