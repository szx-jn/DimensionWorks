//go:build linux

package main

import "errors"

func discoverRegisteredLaunchers() ([]Launcher, error) {
	return nil, errors.New("Linux 仅支持服务端更新，不提供客户端启动器导入")
}
func resolveCustomLauncher(path string) (Launcher, error) {
	return Launcher{}, errors.New("Linux 仅支持服务端更新，不提供自定义客户端启动器")
}
func openMRPackWithLauncher(launcher Launcher, packagePath string) error {
	return errors.New("Linux 仅支持服务端更新，不提供 MRPack 启动器导入")
}

func revealMRPack(packagePath string) error {
	return errors.New("Linux 仅支持服务端更新，不提供客户端差量包展示")
}
