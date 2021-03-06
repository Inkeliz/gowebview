// +build ignore

package main

import (
	"bytes"
	"encoding/hex"
	"go/format"
	"golang.org/x/crypto/blake2b"
	"io/ioutil"
	"text/template"
)

type file struct {
	Location string
	Name     string
	Content  string
	Hash     string
}

type container struct {
	OS     string
	Output string
	Files  []file
}

var base = template.Must(template.New("").Parse(`// +build {{.OS}}
// Code generated by go generate; DO NOT EDIT.

package gowebview

var Files = map[string][]byte{ 
{{range .Files}}	"{{.Name}}": { {{.Content}} },
{{end}}
}

var FilesHashes = map[string][]byte{ 
{{range .Files}}	"{{.Name}}": { {{.Hash}} },
{{end}}
}
`))

func main() {
	var containers = []container{
		{
			OS:     "windows",
			Output: "lib_window.go",
			Files:  []file{{Location: "libs/WebView2Loader.dll", Name: "WebView2Loader.dll"}},
		},
		{
			OS:     "linux",
			Output: "lib_linux.go",
			Files:  []file{{Location: "libs/libwebview.so", Name: "libwebview.so"}},
		},
		{
			OS:     "darwin",
			Output: "lib_darwin.go",
			Files:  []file{{Location: "libs/libwebview.dylib", Name: "libwebview.dylib"}},
		},
	}

	for _, c := range containers {

		for n, f := range c.Files {
			bin, err := ioutil.ReadFile(f.Location)
			if err != nil {
				panic(err)
			}

			hash := blake2b.Sum256(bin)

			c.Files[n].Content = binToByteArrayHex(bin[:])
			c.Files[n].Hash = binToByteArrayHex(hash[:])
		}

		out := bytes.NewBuffer(nil)
		if err := base.Execute(out, c); err != nil {
			panic(err)
		}

		outFormatted, err := format.Source(out.Bytes())
		if err != nil {
			panic(err)
		}

		if err := ioutil.WriteFile(c.Output, outFormatted, 0755); err != nil {
			panic(err)
		}
	}

}

// binToByteArrayHex creates the "Golang Hex", in order to use something like `var X = []byte {0x01, 0x02, 0x03}`
func binToByteArrayHex(bin []byte) string {
	h := hex.EncodeToString(bin)
	data := make([]byte, len(bin)*5)
	for i, ii := 0, 0; i < len(h); i, ii = i+2, ii+5 {
		copy(data[ii:], []byte{0x30, 0x78, h[i], h[i+1], 0x2C})
	}

	return string(data)
}
